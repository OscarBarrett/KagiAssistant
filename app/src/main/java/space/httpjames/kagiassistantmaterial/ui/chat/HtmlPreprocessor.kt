package space.httpjames.kagiassistantmaterial.ui.chat

import org.jsoup.Jsoup
import org.jsoup.nodes.Node

object HtmlPreprocessor {

    fun preprocess(html: String): String {
        if (html.isBlank()) return html

        var processedHtml = html
        processedHtml = removeTooltips(processedHtml)
        processedHtml = preprocessDetailsSummary(processedHtml)
        processedHtml = preprocessImages(processedHtml)
        processedHtml = preprocessTables(processedHtml)
        processedHtml = injectStyles(processedHtml)
        return processedHtml
    }

    private fun removeTooltips(html: String): String {
        val doc = Jsoup.parseBodyFragment(html)
        doc.select("span.tooltip").remove()
        return doc.body().html()
    }

    private fun preprocessDetailsSummary(html: String): String {
        val doc = Jsoup.parseBodyFragment(html)
        val detailsElements = doc.select("details")

        detailsElements.forEach { detailsElement ->
            detailsElement.selectFirst("summary")?.let { summaryElement ->
                var summaryText = summaryElement.text()

                if (summaryText.startsWith("Gathering") || summaryText.startsWith("Gathered")) {
                    val fromIndex = summaryText.indexOf("from")
                    if (fromIndex != -1) {
                        summaryText = summaryText.substring(0, fromIndex).trim()
                    }
                }

                if (summaryText.startsWith("Searching") || summaryText.startsWith("Searched")) {
                    val kagiIndex = summaryText.indexOf("Kagi")
                    if (kagiIndex != -1) {
                        summaryText = summaryText.substring(0, kagiIndex + 4).trim()
                    }
                }

                if (summaryText.contains(":")) {
                    summaryText = summaryText.split(":")[0].trim()
                }

                summaryElement.text(summaryText)

                val blockquote = doc.createElement("blockquote")
                val nodesToMove = mutableListOf<Node>()
                var nextNode = summaryElement.nextSibling()
                while (nextNode != null) {
                    nodesToMove.add(nextNode)
                    nextNode = nextNode.nextSibling()
                }
                nodesToMove.forEach {
                    it.remove()
                    blockquote.appendChild(it)
                }
                detailsElement.appendChild(blockquote)
            }
        }

        val bodyHtmlForCheck = doc.body().html()
        for (detailsEl in detailsElements.reversed()) {
            val detailsHtml = detailsEl.outerHtml()
            val lastIndex = bodyHtmlForCheck.lastIndexOf(detailsHtml)
            if (lastIndex != -1) {
                val trailingHtml = bodyHtmlForCheck.substring(lastIndex + detailsHtml.length)
                val trailingText = Jsoup.parseBodyFragment(trailingHtml).text().trim()
                if (trailingText.isEmpty()) {
                    detailsEl.selectFirst("summary")?.addClass("in-progress")
                    break
                }
            }
        }

        return doc.body().html()
    }

    private fun preprocessImages(html: String): String {
        val doc = Jsoup.parseBodyFragment(html)
        doc.select("img").forEach { img ->
            val src = img.attr("src")
            if (src.isNotEmpty() && !src.startsWith("http") && !src.startsWith("data:")) {
                img.attr("src", "https://kagi.com$src")
            }
            img.addClass("inbound-image")
        }
        return doc.body().html()
    }

    private fun preprocessTables(html: String): String {
        val doc = Jsoup.parseBodyFragment(html)
        doc.select("table").wrap("<div class='scrollable-table'></div>")
        return doc.body().html()
    }

    private fun injectStyles(html: String): String {
        val doc = Jsoup.parseBodyFragment(html)
        val elementsToStyle = listOf(
            "p", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li", "blockquote", "pre", "hr"
        )
        val selector = elementsToStyle.joinToString(", ")
        doc.select(selector).forEach { element ->
            val existingStyle = element.attr("style").trim()
            val styleToAdd = "margin-bottom: 1rem;"
            val newStyle = if (existingStyle.isNotEmpty()) {
                if (existingStyle.endsWith(';')) "$existingStyle $styleToAdd" else "$existingStyle; $styleToAdd"
            } else {
                styleToAdd
            }
            element.attr("style", newStyle)
        }
        return doc.body().html()
    }
}

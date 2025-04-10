package com.example.client.util;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Collections;

public class MarkdownUtil {

    public static String toHtmlPage(String result) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <title>Markdown 展示回答结果</title>" +
                "    <style>" +
                "        .container {" +
                "            display: flex;" +
                "            justify-content: center;" +
                "            padding: 20px;" +
                "        }" +
                "        .content {" +
                "            max-width: 800px;" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;" +
                "        }" +
                "        pre { background: #f6f8fa; padding: 15px; border-radius: 6px }" +
                "        table { border-collapse: collapse; }" +
                "        td, th { border: 1px solid #999; padding: 8px; }" +
                "        th { background-color: #eee; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='content'>" + parseMarkdownToHtml(result) + "</div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    // 转换Markdown为HTML
    public static String parseMarkdownToHtml(String markdownContent) {
        Parser parser = Parser.builder()
                .extensions(Collections.singletonList(TablesExtension.create()))
                .build();
        Node document = parser.parse(markdownContent);

        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(Collections.singletonList(TablesExtension.create()))
                .build();

        String htmlContent = renderer.render(document);

        return htmlContent;
    }

}

package com.leyu.aicodegenerator.core;

import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;
import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeParserTest {

    @Test
    void parseHtmlCode() {
        String codeContent = """
                SOMETHING NOT IMPORTANT：
                html code
                <!DOCTYPE html>
                <html>
                <head>
                    <title>test page</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>

                SOME DESCRIPTION
                """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
        I have designed a sleek, professional Todo List for you. 
        It features a modern layout and clean typography.

        ```html
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <link rel="stylesheet" href="style.css">
            <title>Task Flow</title>
        </head>
        <body>
            <div class="app-container">
                <h1>Daily Tasks</h1>
                <div class="input-group">
                    <input type="text" id="todo-input" placeholder="What's on your mind?">
                    <button id="add-btn">Add</button>
                </div>
                <ul id="todo-list"></ul>
            </div>
            <script src="script.js"></script>
        </body>
        </html>
        ```

        ### Styling Logic
        ```css
        body {
            background-color: #f8f9fa;
            font-family: 'Inter', sans-serif;
            display: flex;
            justify-content: center;
            padding-top: 100px;
        }
        .app-container { background: #fff; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
        ```

        ### Interactive Behavior
        ```javascript
        document.getElementById('add-btn').addEventListener('click', () => {
            const input = document.getElementById('todo-input');
            console.log('New task added: ' + input.value);
            input.value = '';
        });
        ```

        The project follows a strict separation of concerns. You can find the individual files extracted below.
        """;
        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());
    }
}

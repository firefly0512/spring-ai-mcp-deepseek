// DOM 元素
const chatMessages = document.getElementById('chat-messages');
const userInput = document.getElementById('user-input');
const sendButton = document.getElementById('send-button');
const toolsList = document.getElementById('tools-list');

// 消息历史
let messageHistory = [];

// 聊天记录自动滚动
function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// 添加消息到聊天窗口
function addMessage(content, isUser = false) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user' : 'bot'}`;

    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    messageContent.innerHTML = formatMessage(content);

    messageDiv.appendChild(messageContent);
    chatMessages.appendChild(messageDiv);

    // 记录历史消息
    messageHistory.push({
        role: isUser ? 'user' : 'assistant',
        content: content
    });

    scrollToBottom();
    return messageDiv;
}

// 添加"AI思考中"提示
function addThinkingIndicator() {
    const thinkingDiv = document.createElement('div');
    thinkingDiv.className = 'thinking';
    thinkingDiv.innerHTML = 'AI思考中<span class="dots">...</span>';
    chatMessages.appendChild(thinkingDiv);
    scrollToBottom();

    return thinkingDiv;
}

// 添加工具调用信息到聊天窗口
function addToolCallMessage(toolCall, toolArgument) {
    const toolDiv = document.createElement('div');
    toolDiv.className = 'message tool-call';

    // 创建可折叠的标题
    const toolHeader = document.createElement('div');
    toolHeader.className = 'tool-call-header collapsible';

    // 根据工具调用结果添加成功/失败标识
    const statusIcon = toolCall.error ? '❌' : '✅';
    toolHeader.innerHTML = `<span class="tool-icon">🔧</span> <b>工具调用:</b> ${toolCall.name} <span class="status-icon">${statusIcon}</span> <span class="collapse-icon">▶</span>`;

    // 创建内容容器 (默认隐藏)
    const toolContent = document.createElement('div');
    toolContent.className = 'tool-call-content';
    toolContent.style.display = 'none'; // 默认折叠

    // 添加参数信息
    const toolInput = document.createElement('div');
    toolInput.className = 'tool-call-input';
    if (toolArgument.arguments) {
        toolInput.innerHTML = `<b>参数:</b> <pre>${formatJSON(JSON.parse(toolArgument.arguments))}</pre>`;
    } else {
        toolInput.innerHTML = `<b>参数:</b> <pre>{}</pre>`;
    }
    toolContent.appendChild(toolInput);

    // 添加结果信息
    const toolResult = document.createElement('div');
    toolResult.className = 'tool-call-result';

    if (toolCall.error) {
        toolResult.innerHTML = `<b>错误:</b> <span class="error">${toolCall.error}</span>`;
    } else {
        // 尝试解析并格式化结果
        try {
            let formattedResult = '';
            const result = JSON.parse(toolCall.responseData);

            // 检查是否有content字段
            if (result && Array.isArray(result)) {
                // 提取text内容
                let textItems = result
                    .filter(item => item.type === 'text')
                    .map(item => item.text);

                if(!textItems || textItems.length === 0) {
                    console.log('新版本，没有type');
                    textItems = result;
                }

                if (textItems.length > 0) {
                    try {
                        // 尝试解析text内容中的JSON
                        const parsedData = JSON.parse(textItems[0]);
                        formattedResult = formatParsedResult(parsedData);
                    } catch (e) {
                        // 如果不是有效的JSON，直接显示文本
                        formattedResult = textItems.join('<br>');
                    }
                } else {
                    formattedResult = formatJSON(result);
                }
            } else {
                formattedResult = formatJSON(result);
            }

            toolResult.innerHTML = `<b>结果:</b> <div class="formatted-result">${formattedResult}</div>`;
        } catch (e) {
            toolResult.innerHTML = `<b>结果:</b> <pre>${formatJSON(toolCall.responseData || {})}</pre>`;
        }
    }
    toolContent.appendChild(toolResult);

    toolDiv.appendChild(toolHeader);
    toolDiv.appendChild(toolContent);
    chatMessages.appendChild(toolDiv);

    // 添加折叠功能
    toolHeader.addEventListener('click', () => {
        toolHeader.classList.toggle('collapsed');
        const icon = toolHeader.querySelector('.collapse-icon');
        if (toolContent.style.display === 'none') {
            toolContent.style.display = 'block';
            icon.textContent = '▼';
        } else {
            toolContent.style.display = 'none';
            icon.textContent = '▶';
        }
    });

    // 默认添加折叠状态
    toolHeader.classList.add('collapsed');

    scrollToBottom();
    return toolDiv;
}

// 格式化JSON对象为HTML
function formatJSON(obj) {
    return JSON.stringify(obj, null, 2)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
            let cls = 'json-number';
            if (/^"/.test(match)) {
                if (/:$/.test(match)) {
                    cls = 'json-key';
                    // 删除末尾的冒号
                    match = match.replace(/:$/, '');
                } else {
                    cls = 'json-string';
                }
            } else if (/true|false/.test(match)) {
                cls = 'json-boolean';
            } else if (/null/.test(match)) {
                cls = 'json-null';
            }
            return '<span class="' + cls + '">' + match + '</span>';
        })
        .replace(/\n/g, '<br>')
        .replace(/\s{2}/g, '&nbsp;&nbsp;');
}

// 格式化解析后的数据
function formatParsedResult(data) {
    // 处理数组
    if (Array.isArray(data)) {
        if (data.length === 0) return '<div class="empty-result">[]</div>';

        return `<div class="result-array">
            ${data.map((item, index) => `
                <div class="result-item">
                    <div class="result-item-header">${index + 1}</div>
                    <div class="result-item-content">
                        ${typeof item === 'object' && item !== null
            ? Object.entries(item).map(([key, value]) => `
                                <div class="result-property">
                                    <strong>${key}:</strong> 
                                    ${formatPropertyValue(value)}
                                </div>`).join('')
            : item}
                    </div>
                </div>
            `).join('')}
        </div>`;
    }
    // 处理对象
    else if (typeof data === 'object' && data !== null) {
        const entries = Object.entries(data);
        if (entries.length === 0) return '<div class="empty-result">{}</div>';

        return `<div class="result-object">
            ${entries.map(([key, value]) => `
                <div class="result-property">
                    <strong>${key}:</strong> 
                    ${formatPropertyValue(value)}
                </div>
            `).join('')}
        </div>`;
    }
    // 处理原始值
    else {
        return String(data);
    }
}

// 格式化属性值
function formatPropertyValue(value) {
    if (typeof value === 'object' && value !== null) {
        if (Array.isArray(value)) {
            if (value.length === 0) return '[]';
            if (value.length <= 3 && value.every(v => typeof v !== 'object')) {
                return `[${value.join(', ')}]`;
            }
            return `<span class="expandable-value" onclick="this.classList.toggle('expanded')">
                [数组: ${value.length}项] <span class="preview-content">${formatJSON(value)}</span>
            </span>`;
        } else {
            const keys = Object.keys(value);
            if (keys.length === 0) return '{}';
            if (keys.length <= 2 && keys.every(k => typeof value[k] !== 'object')) {
                return `{${keys.map(k => `${k}: ${value[k]}`).join(', ')}}`;
            }
            return `<span class="expandable-value" onclick="this.classList.toggle('expanded')">
                {对象: ${keys.length}个属性} <span class="preview-content">${formatJSON(value)}</span>
            </span>`;
        }
    }
    return String(value);
}

// 格式化消息内容，支持基本Markdown
function formatMessage(content) {
    // 处理代码块
    content = content.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');

    // 处理行内代码
    content = content.replace(/`([^`]+)`/g, '<code>$1</code>');

    // 处理粗体
    content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // 处理斜体
    content = content.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // 处理链接
    content = content.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank">$1</a>');

    // 处理换行
    content = content.replace(/\n/g, '<br>');

    return content;
}

// 获取所有可用工具
async function loadTools() {
    try {
        const response = await fetch(baseUrl + '/api/chat/tools');
        const data = await response.json();

        console.log(data);

        // 清空加载中提示
        toolsList.innerHTML = '';

        // 显示工具列表
        if (data && data.length > 0) {
            data.forEach(tool => {
                const toolDiv = document.createElement('div');
                toolDiv.className = 'tool-item';

                const toolName = document.createElement('div');
                toolName.className = 'tool-name';
                toolName.textContent = tool.name;

                const toolDescription = document.createElement('div');
                toolDescription.className = 'tool-description';
                toolDescription.textContent = tool.description || '无描述';

                toolDiv.appendChild(toolName);
                toolDiv.appendChild(toolDescription);

                // 添加参数信息
                const inputTypeSchema = JSON.parse(tool.inputTypeSchema);
                if (inputTypeSchema && inputTypeSchema.properties) {
                    const paramsDiv = document.createElement('div');
                    paramsDiv.className = 'tool-params';

                    Object.entries(inputTypeSchema.properties).forEach(([paramName, paramInfo]) => {
                        const paramDiv = document.createElement('div');

                        // 处理基本参数
                        if (paramInfo.description) {
                            paramDiv.innerHTML = `<span class="param-name">${paramName}</span>: ${paramInfo.description}`;
                            paramsDiv.appendChild(paramDiv);
                        }

                        // 处理数组类型的参数
                        if (paramInfo.type === 'array' && paramInfo.items && paramInfo.items.properties) {
                            const nestedParamsDiv = document.createElement('div');
                            nestedParamsDiv.className = 'nested-params';

                            Object.entries(paramInfo.items.properties).forEach(([nestedName, nestedInfo]) => {
                                const nestedParamDiv = document.createElement('div');
                                nestedParamDiv.className = 'nested-param';
                                nestedParamDiv.innerHTML = `<span class="param-name">${paramName}.${nestedName}</span>: ${nestedInfo.description || nestedName}`;
                                nestedParamsDiv.appendChild(nestedParamDiv);
                            });

                            paramsDiv.appendChild(nestedParamsDiv);
                        }
                    });

                    toolDiv.appendChild(paramsDiv);
                }

                toolsList.appendChild(toolDiv);
            });
        } else {
            toolsList.innerHTML = '<div class="loading">没有可用的工具</div>';
        }
    } catch (error) {
        console.error('加载工具失败:', error);
        toolsList.innerHTML = '<div class="loading error">加载工具失败</div>';
    }
}

// 发送消息到服务器
async function sendMessage(message) {
    try {
        // 显示用户消息
        addMessage(message, true);

        // 显示思考中指示器
        const thinkingIndicator = addThinkingIndicator();

        // 构建API URL
        const apiUrl = new URL(baseUrl + '/api/chat/generate_stream');
        apiUrl.searchParams.append('id', '01');
        apiUrl.searchParams.append('prompt', message);
        // history: messageHistory.slice(0, -1) // 不包括刚刚添加的用户消息

        // 创建EventSource连接
        const eventSource = new EventSource(apiUrl);
        let aiMessageElement = null;
        let accumulatedMarkdown = '';

        eventSource.addEventListener('message', event => {
            // 移除思考中指示器
            thinkingIndicator.remove();
        });

        eventSource.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log(data);

                const messageType = data.messageType || '';
                const content = data.text || '';
                const finishReason = data.metadata?.finishReason;
                const toolCalls = data.responses;
                const toolArguments = data.metadata?.toolArguments;

                // 如果有工具调用
                if (messageType && messageType === 'TOOL') {

                    console.log('创建消息容器 tool');

                    // 添加工具调用提示
                    const toolsUsedDiv = document.createElement('div');
                    toolsUsedDiv.className = 'message system';
                    toolsUsedDiv.textContent = `AI 正在使用 ${toolCalls.length} 个工具来回答您的问题...`;
                    chatMessages.appendChild(toolsUsedDiv);

                    // 显示每个工具调用
                    if (toolCalls && toolArguments && toolCalls.length === toolArguments.length) {
                        for (let i = 0; i < toolCalls.length; i++) {
                            const currentToolCall = toolCalls[i];
                            const currentToolArgument = toolArguments[i];
                            addToolCallMessage(currentToolCall, currentToolArgument);
                        }
                    } else {
                        console.log('toolCalls 和 toolArguments 长度不一致或数据不存在');
                    }

                    // 添加工具使用完成提示
                    const toolsCompletedDiv = document.createElement('div');
                    toolsCompletedDiv.className = 'message system';
                    toolsCompletedDiv.textContent = `工具使用完成，AI 正在生成最终回复...`;
                    chatMessages.appendChild(toolsCompletedDiv);
                }

                // 添加AI回复
                if (content && content.length > 0) {
                    // 创建消息容器（如果不存在）
                    if (!aiMessageElement && content.replaceAll('\n', '').length > 0) {
                        console.log('创建消息容器 ai');
                        aiMessageElement = addMessage('');
                    }
                    if(aiMessageElement) {
                        accumulatedMarkdown += content;
                        aiMessageElement.querySelector('.message-content').innerHTML = formatMessage(accumulatedMarkdown);
                        scrollToBottom();
                    }
                }

                // 处理结束
                if (finishReason === 'STOP') {
                    eventSource.close();
                }

            } catch (error) {
                console.error('解析错误:', error);
                eventSource.close();

                // 移除思考中指示器(如果存在)
                document.querySelector('.thinking')?.remove();
                addMessage(`解析错误: ${error || '未知错误'}`);
            }
        };

        eventSource.onerror = (error) => {
            console.error('连接错误:', error);
            eventSource.close();

            // 移除思考中指示器(如果存在)
            document.querySelector('.thinking')?.remove();
            addMessage('对话连接异常，请重试');
        };

    } catch (error) {
        console.error('发送消息出错:', error);

        addMessage(`发送消息出错: ${error.message}`);
    }
}

// 发送消息事件处理
function handleSend() {
    const message = userInput.value.trim();
    if (message) {
        sendMessage(message);
        userInput.value = '';
    }
}

// 绑定事件
sendButton.addEventListener('click', handleSend);

userInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
    }
});

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', () => {
    loadTools();
    userInput.focus();
});
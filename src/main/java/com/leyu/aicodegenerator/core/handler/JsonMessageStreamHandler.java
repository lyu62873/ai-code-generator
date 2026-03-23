package com.leyu.aicodegenerator.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.ai.model.message.AiResponseMessage;
import com.leyu.aicodegenerator.ai.model.message.StreamMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolRequestMessage;
import com.leyu.aicodegenerator.ai.tools.BaseTool;
import com.leyu.aicodegenerator.ai.tools.ToolManager;
import com.leyu.aicodegenerator.entity.ChatHistoryOriginal;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.leyu.aicodegenerator.model.enums.StreamMessageTypeEnum;
import com.leyu.aicodegenerator.service.ChatHistoryOriginalService;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Handle incoming streaming chunks and update chat history (incl. tool calls). */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private ChatHistoryService  chatHistoryService;
    @Resource
    private ChatHistoryOriginalService  chatHistoryOriginalService;
    @Autowired
    private ToolManager toolManager;


    public Flux<String> handle(Flux<String> originFlux,
                               long appId, User loginUser) {
        StringBuilder chatHistoryStringBuilder = new StringBuilder();

        StringBuilder aiResponseStringBuilder = new StringBuilder();
        List<ChatHistoryOriginal> originalChatHistoryList = new ArrayList<>();
        Set<String> usedToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> handleJsonMessageChunk(chunk, chatHistoryStringBuilder, aiResponseStringBuilder, originalChatHistoryList, usedToolIds))
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    // Persist tool call info
                    if (!originalChatHistoryList.isEmpty()) {
                        // Enrich ChatHistoryOriginal info
                        originalChatHistoryList.forEach(chatHistory -> {
                            chatHistory.setAppId(appId);
                            chatHistory.setUserId(loginUser.getId());
                        });
                        // Batch persist
                        chatHistoryOriginalService.addOriginalChatMessageBatch(originalChatHistoryList);
                    }
                    // Persist AI response (two cases: 1) no tool call; 2) after tool calls, the AI usually returns one more message)
                    String aiResponseStr = aiResponseStringBuilder.toString();
                    chatHistoryOriginalService.addOriginalChatMessage(appId, aiResponseStr, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());

                    // After streaming completes, add the AI message to chat history
                    String chatHistoryStr = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, chatHistoryStr, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    String errorMessage = "AI response error: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    chatHistoryOriginalService.addOriginalChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }




    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder,
                                          StringBuilder aiResponseStringBuilder,
                                          List<ChatHistoryOriginal> originalChatHistoryList,
                                          Set<String> usedToolIds) {
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                if (StrUtil.isBlank(data)) {
                    return "";
                }
                chatHistoryStringBuilder.append(data);
                aiResponseStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();

                // DeepSeek may return name=null during the partial phase; ignore it
                if (StrUtil.isBlank(toolName)) {
                    return "";
                }

                String dedupKey = buildToolDedupKey(toolId, toolName);
                if (usedToolIds.contains(dedupKey)) {
                    return "";
                }
                usedToolIds.add(dedupKey);

                BaseTool tool = toolManager.getTool(toolName);
                if (tool != null) {
                    return tool.generateToolRequestResponse();
                }

                // For non-BaseTool (or unregistered tools), return a generic response to avoid NPE
                return generateGenericToolRequestResponse(toolName);
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);

                // An empty name usually means dirty data in the chunk; ignore it
                if (StrUtil.isBlank(toolExecutedMessage.getName())) {
                    return "";
                }

                // Persist only complete tool execution results to avoid polluting original history
                processToolExecutionMessage(aiResponseStringBuilder, chunk, originalChatHistoryList);

                String toolName = toolExecutedMessage.getName();
                BaseTool tool = toolManager.getTool(toolName);

                String result;
                if (tool != null) {
                    JSONObject jsonObject = StrUtil.isNotBlank(toolExecutedMessage.getArguments())
                            ? JSONUtil.parseObj(toolExecutedMessage.getArguments())
                            : new JSONObject();
                    result = tool.generateToolExecutedResult(jsonObject);
                } else {
                    result = generateGenericToolExecutedResult(toolExecutedMessage);
                }

                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("Unsupported MessageType: {}", typeEnum);
                return "";
            }
        }
    }

/** Build Tool Dedup Key. */
    private String buildToolDedupKey(String toolId, String toolName) {
        if (StrUtil.isNotBlank(toolId)) {
            return "id:" + toolId;
        }
        return "name:" + StrUtil.blankToDefault(toolName, "unknown");
    }

/** Generate output for the request (and persist/upload as needed). */
    private String generateGenericToolRequestResponse(String toolName) {
        return switch (toolName) {
            case "searchContentImages" -> "\n\n[Select Tool] Search Content Images\n\n";
            case "searchIllustrations" -> "\n\n[Select Tool] Search Illustrations\n\n";
            case "generateMermaidDiagram" -> "\n\n[Select Tool] Generate Mermaid Diagram\n\n";
            default -> String.format("\n\n[Select Tool] %s\n\n", toolName);
        };
    }

/** Generate output for the request (and persist/upload as needed). */
    private String generateGenericToolExecutedResult(ToolExecutedMessage msg) {
        String toolName = StrUtil.blankToDefault(msg.getName(), "unknown_tool");
        String args = StrUtil.blankToDefault(msg.getArguments(), "{}");
        return String.format("[Tool Executed] %s %s", toolName, args);
    }

    private void processToolExecutionMessage(StringBuilder aiResponseStringBuilder,
                                             String chunk,
                                             List<ChatHistoryOriginal> originalChatHistoryList) {
        ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);

        // Don't persist when key fields are incomplete; avoid replay errors
        if (StrUtil.isBlank(toolExecutedMessage.getName())
                || StrUtil.isBlank(toolExecutedMessage.getId())) {
            return;
        }

        String aiResponseStr = aiResponseStringBuilder.toString();

        ToolRequestMessage toolRequestMessage = new ToolRequestMessage();
        toolRequestMessage.setId(toolExecutedMessage.getId());
        toolRequestMessage.setName(toolExecutedMessage.getName());
        toolRequestMessage.setArguments(StrUtil.blankToDefault(toolExecutedMessage.getArguments(), "{}"));
        toolRequestMessage.setText(aiResponseStr);

        String toolRequestJsonStr = JSONUtil.toJsonStr(toolRequestMessage);

        ChatHistoryOriginal toolRequestHistory = ChatHistoryOriginal.builder()
                .message(toolRequestJsonStr)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_REQUEST.getValue())
                .build();
        originalChatHistoryList.add(toolRequestHistory);

        ChatHistoryOriginal toolResultHistory = ChatHistoryOriginal.builder()
                .message(chunk)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT.getValue())
                .build();
        originalChatHistoryList.add(toolResultHistory);

        // Clear the current AI text buffer after tool execution (keep your original semantics)
        aiResponseStringBuilder.setLength(0);
    }
}

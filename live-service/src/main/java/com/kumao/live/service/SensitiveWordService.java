package com.kumao.live.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 简易敏感词过滤（DFA 前缀树）
 */
@Service
public class SensitiveWordService {

    @Value("${kumao.danmaku.sensitive-words:}")
    private String sensitiveWords;

    private final TrieNode root = new TrieNode();

    @PostConstruct
    public void init() {
        if (sensitiveWords == null || sensitiveWords.isBlank()) {
            return;
        }
        for (String word : sensitiveWords.split(",")) {
            String w = word.trim();
            if (!w.isEmpty()) {
                addWord(w);
            }
        }
    }

    public void addWord(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.end = true;
    }

    /**
     * @return 命中敏感词返回 true
     */
    public boolean contains(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            TrieNode node = root;
            int j = i;
            while (j < text.length()) {
                node = node.children.get(text.charAt(j));
                if (node == null) {
                    break;
                }
                if (node.end) {
                    return true;
                }
                j++;
            }
        }
        return false;
    }

    public String filter(String text, String replacement) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            TrieNode node = root;
            int j = i;
            int matchedEnd = -1;
            while (j < text.length()) {
                node = node.children.get(text.charAt(j));
                if (node == null) {
                    break;
                }
                if (node.end) {
                    matchedEnd = j;
                }
                j++;
            }
            if (matchedEnd >= i) {
                for (int k = i; k <= matchedEnd; k++) {
                    sb.setCharAt(k, '*');
                }
                i = matchedEnd;
            }
        }
        return sb.toString();
    }

    private static class TrieNode {
        private final java.util.Map<Character, TrieNode> children = new java.util.HashMap<>();
        private boolean end;
    }
}

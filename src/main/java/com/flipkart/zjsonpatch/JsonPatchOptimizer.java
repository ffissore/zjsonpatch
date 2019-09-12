package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flipkart.zjsonpatch.Operation.*;

public class JsonPatchOptimizer {
    public static JsonNode optimize(ArrayNode patch) {
        List<JsonNode> optimizedOperations = new ArrayList<JsonNode>();

        Map<String, List<JsonNode>> operationsPerPath = new HashMap<String, List<JsonNode>>();

        for (JsonNode operation : patch) {
            String path = operation.get("path").asText();
            if (operationsPerPath.get(path) == null) {
                addOperation(optimizedOperations, operationsPerPath, operation);
            } else {
                System.out.println(operationsPerPath);
                if (isOp(REMOVE, operation)) {
                    List<JsonNode> previousOperations = operationsPerPath.get(path);
                    for (int i = previousOperations.size() - 1; i >= 0; i--) {
                        JsonNode previousOperation = previousOperations.get(i);
                        if (isOp(ADD, previousOperation) || (isOp(COPY, previousOperation) && stringEquals(previousOperation, operation, "path"))) {

                            optimizedOperations.remove(previousOperation);
                            previousOperations.remove(previousOperation);

                            for (int j = i; j < previousOperations.size(); j++) {
                                ObjectNode subsequentPreviousOperation = (ObjectNode) previousOperations.get(j);
                                if (isOp(COPY, subsequentPreviousOperation) && stringEquals(subsequentPreviousOperation, previousOperation, "from")) {
                                    subsequentPreviousOperation.put("op", ADD.rfcName());
                                    subsequentPreviousOperation.remove("from");
                                    subsequentPreviousOperation.set("value", previousOperation.get("value"));
                                }
                            }
                            break;
                        }
                    }
                } else {
                    addOperation(optimizedOperations, operationsPerPath, operation);
                }
            }
        }

        System.out.println(operationsPerPath);

        ArrayNode optimizedPatch = patch.arrayNode(optimizedOperations.size());
        for (JsonNode op : optimizedOperations) {
            optimizedPatch.add(op);
        }

        return optimizedPatch;
    }

    private static boolean stringEquals(JsonNode node1, JsonNode node2, String path) {
        return node1.get(path).asText().equals(node2.get("path").asText());
    }

    private static boolean isOp(Operation op, JsonNode node) {
        return op.rfcName().equals(node.get("op").asText());
    }

    private static void addOperation(List<JsonNode> optimizedOperations, Map<String, List<JsonNode>> operationsPerPath, JsonNode operation) {
        String path = operation.get("path").asText();

        ensureHasList(operationsPerPath, path);

        operationsPerPath.get(path).add(operation);
        optimizedOperations.add(operation);

        if (isOp(COPY, operation)) {
            path = operation.get("from").asText();
            ensureHasList(operationsPerPath, path);
            operationsPerPath.get(path).add(operation);
        }
    }

    private static void ensureHasList(Map<String, List<JsonNode>> operationsPerPath, String path) {
        if (!operationsPerPath.containsKey(path)) {
            operationsPerPath.put(path, new ArrayList<JsonNode>());
        }
    }
}

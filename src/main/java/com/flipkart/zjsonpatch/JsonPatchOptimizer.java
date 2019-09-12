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
        List<ObjectNode> optimizedOperations = new ArrayList<ObjectNode>();

        Map<String, List<ObjectNode>> operationsPerPath = new HashMap<String, List<ObjectNode>>();

        for (JsonNode operation : patch) {
            String path = operation.get("path").asText();
            if (operationsPerPath.get(path) == null) {
                operationsPerPath.put(path, new ArrayList<ObjectNode>());
            }

            String from = null;
            if (operation.has("from")) {
                from = operation.get("from").asText();
                if (operationsPerPath.get(from) == null) {
                    operationsPerPath.put(from, new ArrayList<ObjectNode>());
                }
            }

            List<ObjectNode> previousOperationsByPath = operationsPerPath.get(path);
            List<ObjectNode> previousOperationsByFrom = operationsPerPath.get(from);
            if (isOp(REMOVE, operation)) {
                for (int i = previousOperationsByPath.size() - 1; i >= 0; i--) {
                    JsonNode previousOperation = previousOperationsByPath.get(i);
                    if (isOp(ADD, previousOperation) || (isOp(COPY, previousOperation) && stringEquals(previousOperation, operation, "path"))) {

                        optimizedOperations.remove(previousOperation);
                        previousOperationsByPath.remove(previousOperation);

                        for (int j = i; j < previousOperationsByPath.size(); j++) {
                            ObjectNode subsequentPreviousOperation = previousOperationsByPath.get(j);
                            if (isOp(COPY, subsequentPreviousOperation) && stringEquals(subsequentPreviousOperation, previousOperation, "from")) {
                                subsequentPreviousOperation.put("op", ADD.rfcName());
                                subsequentPreviousOperation.remove("from");
                                subsequentPreviousOperation.set("value", previousOperation.get("value"));
                            }
                        }
                        break;
                    }
                }
            } else if (isOp(MOVE, operation) && !previousOperationsByFrom.isEmpty() && isOp(ADD, previousOperationsByFrom.get(previousOperationsByFrom.size() - 1))) {
                ObjectNode previousOperation = previousOperationsByFrom.get(previousOperationsByFrom.size() - 1);
                previousOperation.set("path", operation.get("path"));
            } else {
                operationsPerPath.get(path).add((ObjectNode) operation);
                optimizedOperations.add((ObjectNode) operation);
                if (isOp(COPY, operation) || isOp(MOVE, operation)) {
                    operationsPerPath.get(operation.get("from").asText()).add((ObjectNode) operation);
                }
            }
        }

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

}

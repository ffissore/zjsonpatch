package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.flipkart.zjsonpatch.Operation.*;

public class JsonPatchOptimizer {

    public static abstract class OperationOptimizer {

        abstract protected void optimize(ObjectNode operation, List<ObjectNode> operations, int idx);

        abstract protected Operation getOp();

        public void optimize(List<ObjectNode> operations) {
            for (int i = operations.size() - 1; i >= 1; i--) {
                ObjectNode operation = operations.get(i);

                if (isOp(getOp(), operation)) {
                    optimize(operation, operations, i);
                }
            }
        }

        protected int searchForPreviousOperation(List<ObjectNode> operations, ObjectNode operation, String field, int fromIdx, String targetField) {
            if (!operation.hasNonNull(field)) {
                return -1;
            }

            String path = operation.get(field).asText();
            for (int i = fromIdx - 1; i >= 0; i--) {
                ObjectNode op = operations.get(i);
                if (op != null && op.hasNonNull(targetField) && path.equals(op.get(targetField).asText())) {
                    return i;
                }
            }
            return -1;
        }

        protected void removeOperation(List<ObjectNode> operations, int idx) {
            operations.set(idx, null);
        }
    }

    public static class MoveOperationOptimizer extends OperationOptimizer {

        @Override
        protected void optimize(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForPreviousOperation(operations, operation, "from", idx, "path");
            if (previousIdx == -1) {
                return;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(ADD, previousOperation) || isOp(MOVE, previousOperation)) {
                removeOperation(operations, idx);
                previousOperation.set("path", operation.get("path"));
            }
        }

        @Override
        protected Operation getOp() {
            return MOVE;
        }
    }

    public static class RemoveOperationOptimizer extends OperationOptimizer {

        @Override
        protected void optimize(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForPreviousOperation(operations, operation, "path", idx, "from");
            if (previousIdx == -1) {
                optimizeRemoveAddOrCopy(operation, operations, idx);
                return;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(COPY, previousOperation)) {
                int previousPreviousIdx = searchForPreviousOperation(operations, operation, "path", previousIdx, "path");
                if (previousPreviousIdx == -1 || !isOp(ADD, operations.get(previousPreviousIdx))) {
                    optimizeRemoveAddOrCopy(operation, operations, idx);
                    return;
                }

                ObjectNode addOperation = operations.get(previousPreviousIdx);
                removeOperation(operations, idx);
                removeOperation(operations, previousPreviousIdx);

                previousOperation.set("op", addOperation.get("op"));
                previousOperation.remove("from");
                previousOperation.set("value", addOperation.get("value"));
            }
        }

        private void optimizeRemoveAddOrCopy(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForPreviousOperation(operations, operation, "path", idx, "path");
            if (previousIdx == -1) {
                return;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(ADD, previousOperation) || isOp(COPY, previousOperation)) {
                removeOperation(operations, idx);
                removeOperation(operations, previousIdx);
            }
        }

        @Override
        protected Operation getOp() {
            return REMOVE;
        }
    }

    private static class ReplaceOperationOptimizer extends OperationOptimizer {

        @Override
        protected void optimize(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForPreviousOperation(operations, operation, "path", idx, "path");
            if (previousIdx == -1) {
                return;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(REPLACE, previousOperation) || isOp(ADD, previousOperation)) {
                removeOperation(operations, idx);
                previousOperation.set("value", operation.get("value"));
            }
        }

        @Override
        protected Operation getOp() {
            return REPLACE;
        }
    }

    private final List<OperationOptimizer> operationOptimizers;

    public JsonPatchOptimizer() {
        this(Arrays.asList(new MoveOperationOptimizer(), new ReplaceOperationOptimizer(), new RemoveOperationOptimizer()));
    }

    public JsonPatchOptimizer(List<OperationOptimizer> operationOptimizers) {
        this.operationOptimizers = operationOptimizers;
    }

    public JsonNode optimize(ArrayNode patch) {
        List<ObjectNode> operations = new ArrayList<>();
        for (int i = 0; i < patch.size(); i++) {
            operations.add((ObjectNode) patch.get(i));
        }

        for (OperationOptimizer operationOptimizer : operationOptimizers) {
            operationOptimizer.optimize(operations);
        }

        for (int i = operations.size() - 1; i >= 0; i--) {
            if (operations.get(i) == null) {
                patch.remove(i);
            }
        }

        return patch;
    }

    private static boolean stringEquals(JsonNode node1, JsonNode node2, String path) {
        return node1.get(path).asText().equals(node2.get("path").asText());
    }

    private static boolean isOp(Operation op, JsonNode operation) {
        return operation != null && op.rfcName().equals(operation.get("op").asText());
    }

}

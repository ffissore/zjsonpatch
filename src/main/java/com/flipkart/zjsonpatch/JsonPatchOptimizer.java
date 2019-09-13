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

        abstract protected boolean optimize(ObjectNode operation, List<ObjectNode> operations, int idx);

        abstract protected Operation getOp();

        public boolean optimize(List<ObjectNode> operations) {
            boolean optimizerHasOptimized = false;

            for (int i = operations.size() - 1; i >= 1; i--) {
                ObjectNode operation = operations.get(i);

                if (isOp(getOp(), operation)) {
                    boolean result = optimize(operation, operations, i);
                    optimizerHasOptimized = optimizerHasOptimized || result;
                }
            }

            return optimizerHasOptimized;
        }

        protected int searchForNonTestPreviousOperation(List<ObjectNode> operations, ObjectNode operation, String field, int fromIdx, String targetField) {
            return searchForPreviousOperation(operations, operation, field, fromIdx, targetField, TEST);
        }

        protected int searchForPreviousOperation(List<ObjectNode> operations, ObjectNode operation, String field, int fromIdx, String targetField, Operation filteredOutOperation) {
            if (!operation.hasNonNull(field)) {
                return -1;
            }

            String fieldValue = operation.get(field).asText();
            for (int i = fromIdx - 1; i >= 0; i--) {
                ObjectNode op = operations.get(i);
                if (op != null && (filteredOutOperation == null || !isOp(filteredOutOperation, op)) &&
                    op.hasNonNull(targetField) && fieldValue.equals(op.get(targetField).asText())) {
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
        protected boolean optimize(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForNonTestPreviousOperation(operations, operation, "from", idx, "path");
            if (previousIdx == -1) {
                return false;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(ADD, previousOperation) || isOp(MOVE, previousOperation) || isOp(COPY, previousOperation)) {
                removeOperation(operations, idx);
                previousOperation.set("path", operation.get("path"));
                return true;
            }

            return false;
        }

        @Override
        protected Operation getOp() {
            return MOVE;
        }
    }

    public static class RemoveOperationOptimizer extends OperationOptimizer {

        @Override
        protected boolean optimize(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForNonTestPreviousOperation(operations, operation, "path", idx, "from");
            if (previousIdx == -1) {
                return optimizeRemoveAddOrCopy(operation, operations, idx);
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(COPY, previousOperation)) {
                int previousPreviousIdx = searchForNonTestPreviousOperation(operations, operation, "path", previousIdx, "path");
                if (previousPreviousIdx == -1 || !isOp(ADD, operations.get(previousPreviousIdx))) {
                    return optimizeRemoveAddOrCopy(operation, operations, idx);
                }

                ObjectNode addOperation = operations.get(previousPreviousIdx);
                removeOperation(operations, idx);
                removeOperation(operations, previousPreviousIdx);

                previousOperation.set("op", addOperation.get("op"));
                previousOperation.remove("from");
                previousOperation.set("value", addOperation.get("value"));

                return true;
            }

            return false;
        }

        private boolean optimizeRemoveAddOrCopy(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForNonTestPreviousOperation(operations, operation, "path", idx, "path");
            if (previousIdx == -1) {
                return false;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(ADD, previousOperation) || isOp(COPY, previousOperation)) {
                removeOperation(operations, idx);
                removeOperation(operations, previousIdx);
                return true;
            }

            return false;
        }

        @Override
        protected Operation getOp() {
            return REMOVE;
        }
    }

    private static class ReplaceOperationOptimizer extends OperationOptimizer {

        @Override
        protected boolean optimize(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForNonTestPreviousOperation(operations, operation, "path", idx, "path");
            if (previousIdx == -1) {
                return false;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(REPLACE, previousOperation) || isOp(ADD, previousOperation)) {
                removeOperation(operations, idx);
                previousOperation.set("value", operation.get("value"));
                return true;
            }

            if (isOp(COPY, previousOperation)) {
                removeOperation(operations, idx);
                previousOperation.put("op", ADD.rfcName());
                previousOperation.set("value", operation.get("value"));
                previousOperation.remove("from");
                return true;
            }

            return false;
        }

        @Override
        protected Operation getOp() {
            return REPLACE;
        }
    }

    private static class TestOperationOptimizer extends OperationOptimizer {

        @Override
        protected boolean optimize(ObjectNode operation, List<ObjectNode> operations, int idx) {
            int previousIdx = searchForPreviousOperation(operations, operation, "path", idx, "path", null);
            if (previousIdx == -1) {
                removeOperation(operations, idx);
                return true;
            }

            ObjectNode previousOperation = operations.get(previousIdx);
            if (isOp(TEST, previousOperation)) {
                removeOperation(operations, idx);
                return true;
            }

            return false;
        }

        @Override
        protected Operation getOp() {
            return TEST;
        }
    }

    private final List<OperationOptimizer> operationOptimizers;

    public JsonPatchOptimizer() {
        this(Arrays.asList(new MoveOperationOptimizer(), new ReplaceOperationOptimizer(), new RemoveOperationOptimizer(), new TestOperationOptimizer()));
    }

    public JsonPatchOptimizer(List<OperationOptimizer> operationOptimizers) {
        this.operationOptimizers = operationOptimizers;
    }

    public JsonNode optimize(ArrayNode patch) {
        List<ObjectNode> operations = new ArrayList<>();
        for (int i = 0; i < patch.size(); i++) {
            operations.add((ObjectNode) patch.get(i));
        }

        boolean optimizationOccured = true;
        while (optimizationOccured) {
            optimizationOccured = false;
            for (OperationOptimizer operationOptimizer : operationOptimizers) {
                boolean result = operationOptimizer.optimize(operations);
                optimizationOccured = optimizationOccured || result;
            }
        }

        for (int i = operations.size() - 1; i >= 0; i--) {
            if (operations.get(i) == null) {
                patch.remove(i);
            }
        }

        return patch;
    }

    private static boolean isOp(Operation op, JsonNode operation) {
        return operation != null && op.rfcName().equals(operation.get("op").asText());
    }

}

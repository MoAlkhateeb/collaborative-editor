import java.util.Stack;

public class UndoRedoManager {
    private Stack<String> undoStack;
    private Stack<String> redoStack;

    public UndoRedoManager() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
    }

    public void saveState(String state) {
        undoStack.push(state);
        redoStack.clear(); 
    }

    public String undo() {
        if (undoStack.size() <= 1) {
            return null;
        }

        String currentState = undoStack.pop();
        redoStack.push(currentState);
        return undoStack.peek();
    }

    public String redo() {
        if (redoStack.isEmpty()) {
            return null;
        }

        String nextState = redoStack.pop();
        undoStack.push(nextState);
        return nextState;
    }
}
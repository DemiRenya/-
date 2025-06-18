import java.util.*;
import java.util.regex.*;

public class Main {

    static class ExpressionEvaluator {
        private static final Set<String> OPERATORS = Set.of("+", "-", "*", "/", "^", "//");
        private static final Map<String, Integer> PRECEDENCE = Map.of(
            "+", 1,
            "-", 1,
            "*", 2,
            "/", 2,
            "//", 2,
            "^", 3
        );

        public boolean validateExpression(String expr) {
            expr = expr.trim();
            if (!expr.matches("^[0-9\\-].*[0-9]$")) {  // теперь может начинаться с минуса
                return false;
            }
            int operatorsCount = countOperators(expr);
            return operatorsCount <= 99;
        }

        private int countOperators(String expr) {
            int count = 0;
            Pattern p = Pattern.compile("\\+|\\-|\\*|/|//|\\^");
            Matcher m = p.matcher(expr);
            while (m.find()) count++;
            return count;
        }

        public double evaluate(String expr) throws Exception {
            List<String> tokens = tokenize(expr);
            List<String> rpn = infixToRPN(tokens);
            return evalRPN(rpn);
        }

        private List<String> tokenize(String expr) {
            List<String> tokens = new ArrayList<>();
            int i = 0;
            while (i < expr.length()) {
                char c = expr.charAt(i);

                // Пропускаем пробелы
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                // Если число (с возможным унарным минусом)
                if (c == '-' && (i == 0 || expr.charAt(i - 1) == '(' || OPERATORS.contains(String.valueOf(expr.charAt(i - 1))))) {
                    // унарный минус, читаем число с минусом
                    int start = i;
                    i++; // пропускаем минус
                    while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                    tokens.add(expr.substring(start, i));
                    continue;
                }

                if (Character.isDigit(c) || c == '.') {
                    int start = i;
                    while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                    tokens.add(expr.substring(start, i));
                    continue;
                }

                // операторы
                if (c == '/') {
                    if (i + 1 < expr.length() && expr.charAt(i + 1) == '/') {
                        tokens.add("//");
                        i += 2;
                    } else {
                        tokens.add("/");
                        i++;
                    }
                    continue;
                }

                if (c == '+' || c == '-' || c == '*' || c == '^' || c == '(' || c == ')') {
                    tokens.add(String.valueOf(c));
                    i++;
                    continue;
                }

                // Если символ неизвестный
                throw new RuntimeException("Неизвестный символ: " + c);
            }
            return tokens;
        }

        private List<String> infixToRPN(List<String> tokens) throws Exception {
            List<String> output = new ArrayList<>();
            Deque<String> stack = new ArrayDeque<>();

            for (String token : tokens) {
                if (isNumber(token)) {
                    output.add(token);
                } else if (OPERATORS.contains(token)) {
                    while (!stack.isEmpty() && OPERATORS.contains(stack.peek())) {
                        String opTop = stack.peek();
                        if ((isLeftAssociative(token) && PRECEDENCE.get(token) <= PRECEDENCE.get(opTop)) ||
                            (!isLeftAssociative(token) && PRECEDENCE.get(token) < PRECEDENCE.get(opTop))) {
                            output.add(stack.pop());
                        } else {
                            break;
                        }
                    }
                    stack.push(token);
                } else if (token.equals("(")) {
                    stack.push(token);
                } else if (token.equals(")")) {
                    while (!stack.isEmpty() && !stack.peek().equals("(")) {
                        output.add(stack.pop());
                    }
                    if (stack.isEmpty() || !stack.peek().equals("(")) {
                        throw new Exception("Скобки расставлены неверно");
                    }
                    stack.pop();
                } else {
                    throw new Exception("Неизвестный токен: " + token);
                }
            }

            while (!stack.isEmpty()) {
                String op = stack.pop();
                if (op.equals("(") || op.equals(")")) {
                    throw new Exception("Скобки расставлены неверно");
                }
                output.add(op);
            }
            return output;
        }

        private boolean isNumber(String s) {
            try {
                Double.parseDouble(s);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isLeftAssociative(String op) {
            return !op.equals("^");
        }

        private double evalRPN(List<String> tokens) throws Exception {
            Deque<Double> stack = new ArrayDeque<>();
            for (String token : tokens) {
                if (isNumber(token)) {
                    stack.push(Double.parseDouble(token));
                } else {
                    if (stack.size() < 2) throw new Exception("Ошибка в выражении");

                    double b = stack.pop();
                    double a = stack.pop();

                    switch (token) {
                        case "+" -> stack.push(a + b);
                        case "-" -> stack.push(a - b);
                        case "*" -> stack.push(a * b);
                        case "/" -> {
                            if (b == 0) throw new Exception("Деление на ноль");
                            stack.push(a / b);
                        }
                        case "//" -> {
                            if (b == 0) throw new Exception("Деление на ноль");
                            stack.push((double)((long)a / (long)b));
                        }
                        case "^" -> stack.push(Math.pow(a, b));
                        default -> throw new Exception("Неизвестный оператор " + token);
                    }
                }
            }
            if (stack.size() != 1) throw new Exception("Ошибка вычисления");
            return stack.pop();
        }
    }

    static class ConsoleView {
        private final Scanner scanner = new Scanner(System.in);

        public String getInputExpression() {
            System.out.print("Введите математическое выражение: ");
            return scanner.nextLine();
        }

        public void showResult(double result) {
            System.out.println("Результат: " + result);
        }

        public void showError(String message) {
            System.err.println("Ошибка: " + message);
        }
    }

    static class CalculatorController {
        private final ExpressionEvaluator model;
        private final ConsoleView view;

        public CalculatorController(ExpressionEvaluator model, ConsoleView view) {
            this.model = model;
            this.view = view;
        }

        public void run() {
            try {
                String expr = view.getInputExpression();
                if (!model.validateExpression(expr)) {
                    view.showError("Выражение невалидно. Оно должно начинаться и заканчиваться числом, и содержать не более 100 слагаемых.");
                    return;
                }
                double result = model.evaluate(expr);
                view.showResult(result);
            } catch (Exception e) {
                view.showError(e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        ExpressionEvaluator model = new ExpressionEvaluator();
        ConsoleView view = new ConsoleView();
        CalculatorController controller = new CalculatorController(model, view);

        controller.run();
    }
}

import java.util.*;
import java.util.regex.*;

public class Main {

    static class ExpressionEvaluator {
        private static final Set<String> OPERATORS = Set.of("+", "-", "*", "/", "^", "//", "**", "!");
        private static final Set<String> FUNCTIONS = Set.of("log", "exp");
        private static final Map<String, Integer> PRECEDENCE = Map.of(
            "+", 1,
            "-", 1,
            "*", 2,
            "/", 2,
            "//", 2,
            "^", 3,
            "**", 3,
            "!", 4
        );

        public boolean validateExpression(String expr) {
            expr = expr.trim();
            if (!expr.matches("^[0-9\\-].*[0-9)$]")) {
                return false;
            }
            if (!checkParentheses(expr)) {
                return false;
            }
            int operatorsCount = countOperators(expr);
            return operatorsCount <= 15;
        }

        private boolean checkParentheses(String expr) {
            int balance = 0;
            for (char c : expr.toCharArray()) {
                if (c == '(') balance++;
                if (c == ')') balance--;
                if (balance < 0) return false;
            }
            return balance == 0;
        }

        private int countOperators(String expr) {
            int count = 0;
            Pattern p = Pattern.compile("\\+|\\-|\\*|/|//|\\^|\\*\\*|!");
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

                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                // Проверка функций (log, exp)
                if (Character.isLetter(c)) {
                    int start = i;
                    while (i < expr.length() && Character.isLetter(expr.charAt(i))) i++;
                    String func = expr.substring(start, i);
                    if (FUNCTIONS.contains(func)) {
                        tokens.add(func);
                        continue;
                    }
                    throw new RuntimeException("Неизвестная функция: " + func);
                }

                // Числа с унарным минусом
                if (c == '-' && (i == 0 || expr.charAt(i - 1) == '(' || OPERATORS.contains(String.valueOf(expr.charAt(i - 1))))) {
                    int start = i;
                    i++;
                    while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                    tokens.add(expr.substring(start, i));
                    continue;
                }

                // Числа
                if (Character.isDigit(c) || c == '.') {
                    int start = i;
                    while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                    tokens.add(expr.substring(start, i));
                    continue;
                }

                // Операторы
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

                if (c == '*') {
                    if (i + 1 < expr.length() && expr.charAt(i + 1) == '*') {
                        tokens.add("**");
                        i += 2;
                    } else {
                        tokens.add("*");
                        i++;
                    }
                    continue;
                }

                if (c == '+' || c == '-' || c == '^' || c == '!' || c == '(' || c == ')') {
                    tokens.add(String.valueOf(c));
                    i++;
                    continue;
                }

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
                } else if (FUNCTIONS.contains(token)) {
                    stack.push(token);
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
                    if (stack.isEmpty()) {
                        throw new Exception("Несбалансированные скобки");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && FUNCTIONS.contains(stack.peek())) {
                        output.add(stack.pop());
                    }
                }
            }

            while (!stack.isEmpty()) {
                String op = stack.pop();
                if (op.equals("(")) {
                    throw new Exception("Несбалансированные скобки");
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
            return !op.equals("^") && !op.equals("**") && !op.equals("!");
        }

        private double evalRPN(List<String> tokens) throws Exception {
            Deque<Double> stack = new ArrayDeque<>();
            for (String token : tokens) {
                if (isNumber(token)) {
                    stack.push(Double.parseDouble(token));
                } else if (FUNCTIONS.contains(token)) {
                    if (stack.isEmpty()) throw new Exception("Недостаточно аргументов для функции");
                    double a = stack.pop();
                    switch (token) {
                        case "log" -> stack.push(Math.log(a) / Math.log(2));
                        case "exp" -> stack.push(Math.exp(a));
                        default -> throw new Exception("Неизвестная функция: " + token);
                    }
                } else {
                    if (token.equals("!")) {
                        if (stack.isEmpty()) throw new Exception("Недостаточно аргументов для факториала");
                        double a = stack.pop();
                        if (a < 0 || a != Math.floor(a)) {
                            throw new Exception("Факториал определен только для целых неотрицательных чисел");
                        }
                        stack.push(factorial((int)a));
                        continue;
                    }

                    if (stack.size() < 2) throw new Exception("Недостаточно операндов для оператора " + token);

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
                        case "^", "**" -> stack.push(Math.pow(a, b));
                        default -> throw new Exception("Неизвестный оператор " + token);
                    }
                }
            }
            if (stack.size() != 1) throw new Exception("Ошибка вычисления");
            return stack.pop();
        }

        private double factorial(int n) {
            if (n <= 1) return 1;
            double result = 1;
            for (int i = 2; i <= n; i++) {
                result *= i;
            }
            return result;
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
            while (true) {
                try {
                    String expr = view.getInputExpression();
                    if (expr.equalsIgnoreCase("exit")) {
                        break;
                    }
                    if (!model.validateExpression(expr)) {
                        view.showError("Выражение невалидно. Оно должно начинаться и заканчиваться числом/скобкой, содержать не более 15 операторов и иметь сбалансированные скобки.");
                        continue;
                    }
                    double result = model.evaluate(expr);
                    view.showResult(result);
                } catch (Exception e) {
                    view.showError(e.getMessage());
                }
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

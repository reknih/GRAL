package de.haug.gral;

class CliUtilities {
    /**
     * Toy stack for the parentheses checker
     */
    private static class stack
    {
        int top=-1;
        char[] items = new char[600];

        /**
         * Pushes a new element onto the stack
         * @param x The new element
         */
        void push(char x)
        {
            if (top == 99)
            {
                System.out.println("Stack full");
            }
            else
            {
                items[++top] = x;
            }
        }

        /**
         * Returns and deletes the upmost item from the stack
         * @return The upmost item from the stack
         */
        char pop()
        {
            if (top == -1)
            {
                System.out.println("Underflow error");
                return '\0';
            }
            else
            {
                char element = items[top];
                top--;
                return element;
            }
        }

        boolean isEmpty()
        {
            return top == -1;
        }
    }

    /* Returns true if character1 and character2
       are matching left and right Parenthesis */
    private static boolean isMatchingPair(char character1, char character2)
    {
        if (character1 == '(' && character2 == ')')
            return true;
        else if (character1 == '{' && character2 == '}')
            return true;
        else return character1 == '[' && character2 == ']';
    }

    /**
     * @param exp The expression to check
     * @return Return true if expression has balanced Parenthesis
     */
    private static boolean areParenthesisBalanced(char[] exp)
    {
        /* Declare an empty character stack */
        stack st=new stack();

       /* Traverse the given expression to
          check matching parenthesis */
        for (char c : exp) {
          /*If the exp[i] is a starting
            parenthesis then push it*/
            if (c == '{' || c == '(' || c == '[')
                st.push(c);

          /* If exp[i] is an ending parenthesis
             then pop from stack and check if the
             popped parenthesis is a matching pair*/
            if (c == '}' || c == ')' || c == ']') {

              /* If we see an ending parenthesis without
                 a pair then return false*/
                if (st.isEmpty()) {
                    return false;
                }

             /* Pop the top element from stack, if
                it is not a pair parenthesis of character
                then there is a mismatch. This happens for
                expressions like {(}) */
                else if (!isMatchingPair(st.pop(), c)) {
                    return false;
                }
            }

        }

       /* If there is something left in expression
          then there is a starting parenthesis without
          a closing parenthesis */
        return st.isEmpty();
    }

    /**
     * @param exp The expression to check
     * @return Return true if expression has balanced Parenthesis
     */
    static boolean areParenthesisBalanced(String exp) {
        return areParenthesisBalanced(exp.toCharArray());
    }
}

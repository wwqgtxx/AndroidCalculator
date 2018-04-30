package top.wwqgtxx.calculator;

import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;


import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8RuntimeException;
import com.eclipsesource.v8.V8ScriptExecutionException;
import com.orhanobut.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Calculator Class
 * Created by wwqgtxx on 2018/3/26.
 */

public class Calculator {
    public static final int SET_TEXT_VIEW = 0x01;

    public static final int OPERATOR_ADD = 0x01;
    public static final int OPERATOR_SUB = 0x02;
    public static final int OPERATOR_MUL = 0x03;
    public static final int OPERATOR_DIV = 0x04;
    public static final int OPERATOR_FAC = 0x05;
    public static final int OPERATOR_POW = 0x06;
    public static final int OPERATOR_SIN = 0x07;
    public static final int OPERATOR_COS = 0x08;
    public static final int OPERATOR_BKB = 0x09;
    public static final int OPERATOR_BKE = 0x0A;
    public static final int OPERATOR_QRT = 0x0B;
    public static final int OPERATOR_DEG = 0x0C;

    private static final int MAX_PRECISION = 18;
    private static final int CAL_TIMEOUT_MILLIS = 5000;

    private static class CalculatorImpl implements Handler.Callback {
        static final int DO_CAL = 0x00;
        static final int ADD_NUM = 0x01;
        static final int CLEAR_NUM = 0x02;
        static final int ADD_OPERATOR = 0x03;
        static final int CHANGE_SYMBOL = 0x04;
        static final int ADD_POINT = 0x05;
        static final int CLEAR_BACK = 0x06;
        static final int UPDATE_TEXT_VIEW = 0xff;


        /**
         * thread for calculate
         */
        final HandlerThread handlerThread;
        /**
         * daemon thread for timeout
         */
        final HandlerThread daemonHandlerThread;
        /**
         * handler for update ui
         */
        final Handler uiHandler;
        /**
         * handler for calculate
         */
        final Handler handler;
        /**
         * handler for timeout
         */
        final Handler daemonHandler;
        /**
         * V8 engine for calculate
         */
        V8 v8 = null;
        /**
         * function handle to eval the expr
         */
        V8Function eval = null;
        /**
         * last expr string
         */
        String lastString;
        /**
         * now expr string
         */
        String nowString;

        CalculatorImpl(@NonNull Resources resources, @NonNull Handler uiHandler) {
            this.uiHandler = uiHandler;
            handlerThread = new HandlerThread("CalculatorImpl-Thread") {
                @Override
                public void run() {
                    Logger.d("Thread start!");
                    try {
                        // init V8 engine
                        v8 = V8.createV8Runtime();
                        Logger.d(V8.getV8Version());
//                        NodeJS nodeJS = NodeJS.createNodeJS();
//                        Logger.d(nodeJS.getNodeVersion());
//                        nodeJS.release();
                        // init math.js
                        try (AutoRelease _1 = AutoRelease.obtain(v8)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            try (InputStreamReader inputStreamReader = new InputStreamReader(resources.getAssets().open("math.js"))) {
                                try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                                    String line;
                                    while ((line = bufferedReader.readLine()) != null) {
                                        stringBuilder.append(line);
                                        stringBuilder.append('\n');
                                    }
                                    stringBuilder.append("" +
                                            "math.config({\n" +
                                            "   number: 'BigNumber',  // Choose 'number' (default), 'BigNumber', or 'Fraction'\n" +
                                            "   precision: " + MAX_PRECISION + 2 + "         // 64 by default, only applicable for BigNumbers\n" +
                                            "});\n");
                                    v8.executeVoidScript(stringBuilder.toString(), "math.js", 0);
                                }
                            }
                            // get my_eval()
                            eval = (V8Function) v8.executeObjectScript("" +
                                    "function my_eval(val,precision=" + MAX_PRECISION + "){\n" +
                                    "   val = math.eval(val);\n" +
                                    "   var str = val;\n" +
                                    "   do{\n" +
                                    "      str = math.format(math.round(val,precision),{\n" +
                                    "         notation: 'auto',\n" +
                                    "         precision: precision,\n" +
                                    "         lowerExp: -(precision),\n" +
                                    "         upperExp: precision,\n" +
                                    "      });\n" +
                                    "      precision--;\n" +
                                    "   }while(str.length>" + MAX_PRECISION + ");\n" +
                                    "   return str;\n" +
                                    "};\n" +
                                    "my_eval", "my_eval.js", 0);
                            // run the HandlerThread loop
                            try (AutoRelease _2 = AutoRelease.obtain(eval)) {
                                super.run();
                            }
                        }
                    } catch (Exception e) {
                        Logger.e(e, "error!!!");
                    } finally {
                        Logger.d("Thread exit!");
                    }

                }
            };
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper(), this);

            daemonHandlerThread = new HandlerThread("CalculatorImpl-DaemonThread") {
                @Override
                public void run() {
                    Logger.d("DaemonThread start!");
                    try {
                        super.run();
                    } catch (Exception e) {
                        Logger.e(e, "error!!!");
                    } finally {
                        Logger.d("Thread exit!");
                    }
                }
            };
            daemonHandlerThread.start();
            daemonHandler = new Handler(daemonHandlerThread.getLooper());
            call(CLEAR_NUM);
        }

        @WorkerThread
        void updateTextView() {
            String str_arr[] = new String[2];
            if (nowString.isEmpty()) {
                str_arr[0] = "0";
            } else {
                str_arr[0] = nowString;
            }
            str_arr[1] = lastString;
            uiHandler.sendMessage(uiHandler.obtainMessage(SET_TEXT_VIEW, str_arr));
        }

        @WorkerThread
        void addNum(int num) {
//            Logger.d(num);
            nowString = nowString + num;
        }

        @WorkerThread
        void clearNum() {
            lastString = "";
            nowString = "";
        }

        @WorkerThread
        void clearBack() {
            if (nowString.length() > 0) {
                nowString = nowString.substring(0, nowString.length() - 1);
            }
        }

        @WorkerThread
        void addOperator(int type) {
            switch (type) {
                case OPERATOR_ADD: {
                    nowString = nowString + "+";
                    break;
                }
                case OPERATOR_SUB: {
                    nowString = nowString + "-";
                    break;
                }
                case OPERATOR_MUL: {
                    nowString = nowString + "*";
                    break;
                }
                case OPERATOR_DIV: {
                    nowString = nowString + "/";
                    break;
                }
                case OPERATOR_FAC: {
                    nowString = nowString + "!";
                    break;
                }
                case OPERATOR_POW: {
                    nowString = nowString + "^";
                    break;
                }
                case OPERATOR_SIN: {
                    nowString = nowString + "sin(";
                    break;
                }
                case OPERATOR_COS: {
                    nowString = nowString + "cos(";
                    break;
                }
                case OPERATOR_BKB: {
                    nowString = nowString + "(";
                    break;
                }
                case OPERATOR_BKE: {
                    nowString = nowString + ")";
                    break;
                }
                case OPERATOR_QRT: {
                    nowString = nowString + "√(";
                    break;
                }
                case OPERATOR_DEG: {
                    nowString = nowString + "°";
                    break;
                }
            }
        }

        @WorkerThread
        void doCal() {
            lastString = nowString;
            nowString = "计算中";
            String expr = lastString;
            int where = 0;
            int n = 0;
            // make brackets matches
            while (where < expr.length()) {
                char c = expr.charAt(where);
                if (c == '(') {
                    n++;
                } else if (c == ')') {
                    n--;
                }
                where++;
            }
            if (n > 0) {
                expr += ')';
            }
            lastString = expr;
            // help to calculate '°'
            expr = expr.replaceAll("°", "deg");
            // help to calculate '√('
            where = expr.indexOf("√(");
            try {
                while (where != -1) {
                    int lbegin = where - 1;
                    while (lbegin >= 0) {
                        char c = expr.charAt(lbegin);
                        if (!Character.isDigit(c)) {
                            break;
                        }
                        lbegin--;
                    }
                    int rend = where;
                    n = 0;
                    while (true) {
                        rend++;
                        char c = expr.charAt(rend);
                        if (c == '(') {
                            n++;
                        } else if (c == ')') {
                            n--;
                            if (n == 0) {
                                break;
                            }
                        }
                    }
                    if (lbegin + 1 >= where) {
                        expr = expr.substring(0, lbegin + 1) + "(sqrt" + expr.substring(where + 1, rend + 1) +
                                ")" +
                                expr.substring(rend + 1);
                    } else {
                        expr = expr.substring(0, lbegin + 1) + "(" + expr.substring(where + 1, rend + 1) +
                                "^(1/" +
                                expr.substring(lbegin + 1, where) +
                                "))" +
                                expr.substring(rend + 1);
                    }
                    where = expr.indexOf("√(");
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                nowString = "非法输入，请重新输入!";
                return;
            }
            Logger.d(expr);
            // do real calculate in V8 engine
            try (DoWhenTimeout _1 = DoWhenTimeout.obtain(daemonHandler, this::updateTextView, CAL_TIMEOUT_MILLIS / 10);
                 DoWhenTimeout _2 = DoWhenTimeout.obtain(daemonHandler, v8::terminateExecution, CAL_TIMEOUT_MILLIS)) {
                V8Array para = new V8Array(v8).push(expr);
                try (AutoRelease _3 = AutoRelease.obtain(para)) {
                    Object obj = eval.call(null, para);
                    Logger.d(obj);
                    String str = obj.toString();
                    if (str.equals("Infinity") || str.contains("i")) {
                        throw new NumberFormatException(str);
                    }
                    nowString = str;
                }

            } catch (NumberFormatException e) {
                nowString = "非法计算！";
            } catch (V8ScriptExecutionException e) {
                Logger.w(e.getJSMessage());
                Logger.w(e.getJSStackTrace());
//                e.printStackTrace();
                nowString = "非法输入，请重新输入!";
            } catch (V8RuntimeException e) {
                if (e.getMessage().equals("null")) {
                    Logger.d("timeout!!!");
                    nowString = "计算超时！！！";
                } else {
                    Logger.e(e, "error");
                    nowString = "非法输入，请重新输入!";
                }
            } catch (Exception e) {
                Logger.e(e, "error");
            }
        }

        void changeSymbol() {
            if (!nowString.isEmpty()) {
                if (nowString.charAt(0) != '-') {
                    nowString = "-" + nowString;
                } else {
                    nowString = nowString.substring(1);
                }
            }
        }

        void addPoint() {
            if (nowString.isEmpty()) {
                nowString = "0.";
            } else {
                if (!nowString.endsWith(".")) {
                    nowString = nowString + ".";
                }
            }
        }

        void destroy() {
            daemonHandlerThread.quit();
            handlerThread.quit();
        }

        /**
         * parse the massage from handle
         * @param msg message
         * @return always true
         */
        @Override
        @WorkerThread
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case DO_CAL: {
                    doCal();
                    break;
                }
                case ADD_NUM: {
                    addNum(msg.arg1);
                    break;
                }
                case CLEAR_NUM: {
                    clearNum();
                    break;
                }
                case ADD_OPERATOR: {
                    addOperator(msg.arg1);
                    break;
                }
                case CHANGE_SYMBOL: {
                    changeSymbol();
                    break;
                }
                case ADD_POINT: {
                    addPoint();
                    break;
                }
                case CLEAR_BACK: {
                    clearBack();
                    break;
                }
            }
            updateTextView();
            return true;
        }

        void call(int what) {
            handler.sendEmptyMessage(what);
        }

        void call(int what, int arg1) {
            handler.sendMessage(handler.obtainMessage(what, arg1, 0));
        }
    }

    /**
     * real calculator implement object
     */
    private final CalculatorImpl impl;

    /**
     * init a calculator
     *
     * @param resources the Resources to get assert file
     * @param uiHandler a uiHandler use for updateTextView
     */
    Calculator(@NonNull Resources resources, @NonNull Handler uiHandler) {
        impl = new CalculatorImpl(resources, uiHandler);
    }

    /**
     * add a new num to expr
     *
     * @param num like 0,1,2,3,4,5,6,7,8,9
     */
    @MainThread
    public void addNum(int num) {
        impl.call(CalculatorImpl.ADD_NUM, num);
    }

    /**
     * clear all expr
     */
    @MainThread
    public void clearNum() {
        impl.call(CalculatorImpl.CLEAR_NUM);
    }

    /**
     * clear the last char in expr
     */
    @MainThread
    public void clearBack() {
        impl.call(CalculatorImpl.CLEAR_BACK);
    }

    /**
     * calculate the expr
     */
    @MainThread
    public void doCal() {
        impl.call(CalculatorImpl.DO_CAL);
    }

    /**
     * change the expr symbol form + to - or from - to +
     */
    @MainThread
    public void changeSymbol() {
        impl.call(CalculatorImpl.CHANGE_SYMBOL);
    }

    /**
     * add a point(.) to expr
     */
    @MainThread
    public void addPoint() {
        impl.call(CalculatorImpl.ADD_POINT);
    }

    /**
     * add a new num to expr
     *
     * @param which like Calculator.OPERATOR_ADD
     */
    @MainThread
    public void addOperator(int which) {
        impl.call(CalculatorImpl.ADD_OPERATOR, which);
    }

    /**
     * update the TextView string
     */
    @MainThread
    public void updateTextView() {
        impl.call(CalculatorImpl.UPDATE_TEXT_VIEW);
    }

    /**
     * destroy this calculator
     */
    public void destroy() {
        impl.destroy();
    }
}

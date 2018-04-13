package top.wwqgtxx.calculator;

import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

public class MainActivity extends AppCompatActivity {
    private Calculator calculator;
    private Button buttons[] = new Button[10];
    private TextView textView = null;
    private Handler uiHandler = null;

    static {
        Logger.addLogAdapter(new AndroidLogAdapter());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title);
        setContentView(R.layout.main);

        uiHandler = new Handler((Message msg) -> {
            switch (msg.what) {
                case Calculator.SET_TEXT_VIEW: {
                    if (textView != null) {
//                        Logger.d(textView);
                        SpannableStringBuilder ssb = new SpannableStringBuilder();
                        String str_arr[] = (String[]) msg.obj;
                        ssb.append(str_arr[1], new RelativeSizeSpan(0.5f), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        ssb.append('\n');
                        ssb.append(str_arr[0], new RelativeSizeSpan(1f), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        ssb.append('\n');
                        ssb.append(" ", new RelativeSizeSpan(0.5f), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        textView.setText(ssb);
                    }
                }
            }
            return true;
        });

        calculator = new Calculator(getResources(), uiHandler);

        init();

    }

    private void init() {

        textView = findViewById(R.id.textView);
        for (int i = 0; i < 10; i++) {
            final int ii = i;
            try {
                buttons[ii] = findViewById(R.id.class.getDeclaredField("button_num" + i).getInt(null));
                buttons[ii].setOnClickListener((View v) -> {
//                    Logger.d(ii);
                    calculator.addNum(ii);
                });
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        findViewById(R.id.button_ac).setOnLongClickListener((View v) -> {
            Logger.d("changed!");
            for (int i = 1; i <= 3; i++) {
                ViewGroup.LayoutParams layoutParams1 = buttons[i].getLayoutParams();
                ViewGroup.LayoutParams layoutParams2 = buttons[i + 6].getLayoutParams();
                buttons[i].setLayoutParams(layoutParams2);
                buttons[i + 6].setLayoutParams(layoutParams1);
            }
            return true;
        });
        findViewById(R.id.button_ac).setOnClickListener((View v) -> {
            calculator.clearNum();
        });
        findViewById(R.id.button_back).setOnClickListener((View v) -> {
            calculator.clearBack();
        });
//        findViewById(R.id.button_symbol).setOnClickListener((View v) -> {
//            calculator.changeSymbol();
//        });
        findViewById(R.id.button_point).setOnClickListener((View v) -> {
            calculator.addPoint();
        });
        findViewById(R.id.button_equ).setOnClickListener((View v) -> {
            calculator.doCal();
        });
        findViewById(R.id.button_add).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_ADD);
        });
        findViewById(R.id.button_sub).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_SUB);
        });
        findViewById(R.id.button_mul).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_MUL);
        });
        findViewById(R.id.button_div).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_DIV);
        });
        findViewById(R.id.button_fac).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_FAC);
        });
        findViewById(R.id.button_pow).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_POW);
        });
        findViewById(R.id.button_sin).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_SIN);
        });
        findViewById(R.id.button_cos).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_COS);
        });
        findViewById(R.id.button_brackets_begin).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_BKB);
        });
        findViewById(R.id.button_brackets_end).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_BKE);
        });
        findViewById(R.id.button_qrt).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_QRT);
        });
        findViewById(R.id.button_deg).setOnClickListener((View v) -> {
            calculator.addOperator(Calculator.OPERATOR_DEG);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        calculator.destroy();
    }
}

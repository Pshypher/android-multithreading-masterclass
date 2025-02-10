package com.techyourchance.multithreading.exercises.exercise6;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.techyourchance.multithreading.R;
import com.techyourchance.multithreading.common.BaseFragment;

public class Exercise6Fragment extends BaseFragment implements CalculateFactorialUseCase.Listener {

    public static Fragment newInstance() {
        return new Exercise6Fragment();
    }

    // UI thread
    private EditText mEdtArgument;
    private EditText mEdtTimeout;
    private Button mBtnStartWork;
    private TextView mTxtResult;

    private CalculateFactorialUseCase mCalculateFactorialUseCase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCalculateFactorialUseCase = new CalculateFactorialUseCase();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise_6, container, false);

        mEdtArgument = view.findViewById(R.id.edt_argument);
        mEdtTimeout = view.findViewById(R.id.edt_timeout);
        mBtnStartWork = view.findViewById(R.id.btn_compute);
        mTxtResult = view.findViewById(R.id.txt_result);

        mBtnStartWork.setOnClickListener(v -> {
            if (mEdtArgument.getText().toString().isEmpty()) {
                return;
            }

            mTxtResult.setText("");
            mBtnStartWork.setEnabled(false);


            InputMethodManager imm =
                    (InputMethodManager) requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mBtnStartWork.getWindowToken(), 0);

            int argument = Integer.parseInt(mEdtArgument.getText().toString());
            String timeoutString = mEdtTimeout.getText().toString();
            mCalculateFactorialUseCase.computeFactorial(argument, timeoutString);
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mCalculateFactorialUseCase.registerListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mCalculateFactorialUseCase.unregisterListener(this);
    }

    @Override
    protected String getScreenTitle() {
        return "Exercise 6";
    }

    @Override
    public void onFactorialCalculated(String factorial) {
        mTxtResult.setText(factorial);
        mBtnStartWork.setEnabled(true);
    }
}

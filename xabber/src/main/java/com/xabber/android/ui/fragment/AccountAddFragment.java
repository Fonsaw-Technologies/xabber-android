package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.QRCodeScannerActivity;
import com.xabber.android.ui.dialog.OrbotInstallerDialog;
import com.xabber.android.ui.helper.OrbotHelper;

import java.util.Collections;

public class AccountAddFragment extends Fragment implements View.OnClickListener {

    private CheckBox storePasswordView;
    private CheckBox chkSync;
    private CheckBox chkRequireTLS;
    private CheckBox chkUseTOR;
    private CheckBox createAccountCheckBox;
    private LinearLayout passwordConfirmView;
    private EditText userView;
    private EditText passwordView;
    private EditText passwordConfirmEditText;
    private IntentIntegrator integrator;
    private ImageView qrScan;
    private ImageView clearText;

    public static AccountAddFragment newInstance() {
        return new AccountAddFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_add, container, false);

        storePasswordView = (CheckBox) view.findViewById(R.id.store_password);
        chkSync = (CheckBox) view.findViewById(R.id.chkSync);
        if (XabberAccountManager.getInstance().getAccount() == null) {
            chkSync.setVisibility(View.GONE);
            chkSync.setChecked(false);
        }
        chkRequireTLS = view.findViewById(R.id.chkRequireTLS);
        chkUseTOR = view.findViewById(R.id.chkUseTOR);
        createAccountCheckBox = (CheckBox) view.findViewById(R.id.register_account);
        createAccountCheckBox.setOnClickListener(this);

        clearText = view.findViewById(R.id.imgCross);
        clearText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userView.setText("");
            }
        });

        userView = (EditText) view.findViewById(R.id.account_user_name);
        passwordView = (EditText) view.findViewById(R.id.account_password);
        passwordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals("")) {
                    ((AccountAddActivity)getActivity()).toolbarSetEnabled(false);
                } else {
                    if(!userView.getText().toString().equals(""))
                        ((AccountAddActivity)getActivity()).toolbarSetEnabled(true);
                }
            }
        });
        passwordConfirmEditText = (EditText) view.findViewById(R.id.confirm_password);
        userView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }


            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals("")) {
                    ((AccountAddActivity)getActivity()).toolbarSetEnabled(false);
                    clearText.setVisibility(View.GONE);
                    qrScan.setVisibility(View.VISIBLE);
                } else {
                    clearText.setVisibility(View.VISIBLE);
                    qrScan.setVisibility(View.GONE);
                    if(!passwordView.getText().toString().equals(""))
                        ((AccountAddActivity)getActivity()).toolbarSetEnabled(true);
                }
            }
        });
        passwordConfirmView = (LinearLayout) view.findViewById(R.id.confirm_password_layout);

        qrScan = (ImageView) view.findViewById(R.id.imgQRcode);
        qrScan.setOnClickListener(this);

        ((TextView) view.findViewById(R.id.account_help))
                .setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_account:
                if(createAccountCheckBox.isChecked()) {
                    passwordConfirmView.setVisibility(View.VISIBLE);
                } else {
                    passwordConfirmView.setVisibility(View.GONE);
                }
                break;
            case R.id.imgQRcode:
                //((AccountAddActivity)getActivity()).scanQR();
                integrator = IntentIntegrator.forFragment(this);
                integrator.setOrientationLocked(false)
                        .setBeepEnabled(false)
                        .setCameraId(0)
                        .setPrompt("")
                        .setCaptureActivity(QRCodeScannerActivity.class)
                        .initiateScan(Collections.unmodifiableList(Collections.singletonList(IntentIntegrator.QR_CODE)));
                break;
            default:
                break;
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data){
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode, data);
        if(result!=null){
            if(result.getContents()==null){
                Toast.makeText(getActivity(), "no-go", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Scanned = " + result.getContents(), Toast.LENGTH_LONG).show();
                if(result.getContents().length()>5)
                    if(result.getContents().substring(0,5).equals("xmpp:")) {
                        userView.setText(result.getContents().substring(5));
                        addAccount();
                        //passwordView.requestFocus();
                    }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void addAccount() {
        if (chkUseTOR.isChecked() && !OrbotHelper.isOrbotInstalled()) {
            OrbotInstallerDialog.newInstance().show(getFragmentManager(), OrbotInstallerDialog.class.getName());
            return;
        }

        if (createAccountCheckBox.isChecked() &&
                !passwordView.getText().toString().contentEquals(passwordConfirmEditText.getText().toString())) {
            Toast.makeText(getActivity(), getString(R.string.CONFIRM_PASSWORD), Toast.LENGTH_LONG).show();
            return;
        }

        AccountJid account;
        try {
            account = AccountManager.getInstance().addAccount(
                    userView.getText().toString().trim(),
                    passwordView.getText().toString(),
                    "",
                    false,
                    storePasswordView.isChecked(),
                    chkSync.isChecked(),
                    chkUseTOR.isChecked(),
                    createAccountCheckBox.isChecked(), true,
                    chkRequireTLS.isChecked());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
            return;
        }

        // update remote settings
        if (chkSync.isChecked()) XabberAccountManager.getInstance().updateSettingsWithSaveLastAccount(account);

        getActivity().setResult(Activity.RESULT_OK, AccountAddActivity.createAuthenticatorResult(account));
        startActivity(AccountActivity.createIntent(getActivity(), account));
        getActivity().finish();
    }
}

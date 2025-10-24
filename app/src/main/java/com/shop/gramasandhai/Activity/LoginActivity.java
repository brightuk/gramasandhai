package com.shop.gramasandhai.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.R;
import com.shop.gramasandhai.databinding.ActivityLoginBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private MaterialCardView loginCard;
    private CircularProgressIndicator loadingProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViews();
        setupTextWatchers();
        setupClickListeners();

        // Start with fade in animation for the entire layout
        animateLayoutEntrance();
    }

    private void initializeViews() {
        // Initialize views using view binding
        loginCard = binding.loginCard;
        loadingProgress = binding.loading;

        // Set initial state
        binding.login.setEnabled(false);
        binding.login.setAlpha(0.5f);

        // Hide progress bar initially
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
    }

    private void animateLayoutEntrance() {
        // Animate the header section with fade in
        View header = binding.logo;
        if (header != null) {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            header.startAnimation(fadeIn);
        }

        // Animate the card with delay
        if (loginCard != null) {
            loginCard.setAlpha(0f);
            loginCard.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .start();
        }
    }

    private void setupTextWatchers() {
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateForm();
            }
        };

        binding.username.addTextChangedListener(afterTextChangedListener);
        binding.password.addTextChangedListener(afterTextChangedListener);

        // Handle keyboard done action
        binding.password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && binding.login.isEnabled()) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
    }

    private void validateForm() {
        String username = binding.username.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

        boolean isUsernameValid = !username.isEmpty();
        boolean isPasswordValid = !password.isEmpty() && password.length() >= 3;

        // Update email field
        if (!isUsernameValid && username.length() > 0) {
            binding.phoneLayout.setError("Please enter a valid phone number");
            animateError(binding.phoneLayout);
        } else {
            binding.phoneLayout.setError(null);
        }

        // Update password field
        if (!isPasswordValid && password.length() > 0) {
            binding.passwordLayout.setError("Password must be at least 3 characters");
            animateError(binding.passwordLayout);
        } else {
            binding.passwordLayout.setError(null);
        }

        boolean isFormValid = isUsernameValid && isPasswordValid;
        binding.login.setEnabled(isFormValid);
        binding.login.setAlpha(isFormValid ? 1f : 0.5f);
    }

    private void animateError(View view) {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        view.startAnimation(shake);
    }

    private void setupClickListeners() {
        // Login button with animation
        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add button press animation
                Animation scaleDown = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.button_click);

                v.startAnimation(scaleDown);

                // Attempt login after animation
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        attemptLogin();
                    }
                }, 100);
            }
        });

        // Forgot password with animation
        binding.forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation fadeIn = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.fade_in);
                v.startAnimation(fadeIn);
                showForgotPasswordDialog();
            }
        });

//        // Sign up link with animation
//        binding.signUpLink.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Animation scaleIn = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.fade_in);
//                v.startAnimation(scaleIn);
//                Toast.makeText(LoginActivity.this, "Sign up feature coming soon!", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    private void attemptLogin() {
        if (!binding.login.isEnabled()) {
            return;
        }

        hideKeyboard();
        showLoading();

        String username = binding.username.getText().toString().trim();
        String password = binding.password.getText().toString();

        logIn(username, password);
    }

    private void showLoading() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }
        binding.login.setEnabled(false);
        binding.login.setAlpha(0.5f);

        // Animate the login card if it exists
        if (loginCard != null) {
            loginCard.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(200)
                    .start();
        }
    }

    private void hideLoading() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }

        // Re-enable based on form validity
        validateForm();

        // Restore login card if it exists
        if (loginCard != null) {
            loginCard.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }
    }


    private void showLoginSuccess(String message) {
        // Show success message
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.success_green))
                .show();

        // Animate success and navigate
        animateSuccessThenNavigate();
    }

    private void animateSuccessThenNavigate() {
        // Animate success state
        if (loginCard != null) {
            loginCard.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            loginCard.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(150)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Navigate after animation
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                            finish();
                                        }
                                    })
                                    .start();
                        }
                    })
                    .start();
        } else {
            // Navigate directly if no card animation
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        }
    }

    public void logIn(String username, String password) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("phone", username)
                .add("password", password)
                .build();

        Log.d("LOGIN_ATTEMPT", "Phone: " + username+"Password: " + password);

        Request request = new Request.Builder()
                .url(Attributes.LogIn)
                .post(requestBody)
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoginFailed("Network error occurred");
                });
                Log.e("API_ERROR", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("LOGIN_RESPONSE", responseBodyString);

                runOnUiThread(() -> {
                    hideLoading();

                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            // Success logic
                            JSONObject userData = responseObject.getJSONObject("user");
                            int id = userData.getInt("id");
                            int shopId = userData.getInt("shop_id");
                            String name = userData.getString("shop_name");

                            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("id", String.valueOf(id));
                            editor.putString("shopId", String.valueOf(shopId));
                            editor.putString("name", name);
                            editor.apply();

                            showLoginSuccess("Welcome back, " + name + "!");

                        } else {
                            if(responseObject.optString("error").equals("Invalid credentials")){
                                showLoginFailed("Your username or password is wrong");
                            } else {
                                showLoginFailed("Something went wrong");
                            }
                        }

                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        showLoginFailed("Response parsing error");
                    }
                });
            }
        });
    }

    private void showLoginFailed(String errorMessage) {
        // Add shake animation for failure
        if (loginCard != null) {
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            loginCard.startAnimation(shake);
        }

        Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.error_red))
                .setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.password.requestFocus();
                        showKeyboard();
                    }
                })
                .show();
    }

    private void showForgotPasswordDialog() {
        Toast.makeText(this, "Forgot Password feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        binding.password.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.password, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
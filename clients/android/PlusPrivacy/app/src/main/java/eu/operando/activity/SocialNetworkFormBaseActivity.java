package eu.operando.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import eu.operando.R;
import eu.operando.adapter.FacebookSettingsListAdapter;
import eu.operando.customView.AccordionOnGroupExpandListener;
import eu.operando.customView.FacebookSettingsInfoDialog;
import eu.operando.customView.OperandoProgressDialog;
import eu.operando.models.SocialNetworkEnum;
import eu.operando.models.privacysettings.AvailableSettings;
import eu.operando.models.privacysettings.AvailableSettingsWrite;
import eu.operando.models.privacysettings.OspSettings;
import eu.operando.models.privacysettings.Preference;
import eu.operando.models.privacysettings.Question;
import eu.operando.swarmService.models.GetUserPreferencesSwarm;
import eu.operando.swarmService.models.PrivacyWizardSwarm;
import eu.operando.swarmService.models.SaveUserPreferencesSwarm;
import eu.operando.swarmclient.SwarmClient;
import eu.operando.swarmclient.models.PrivacyWizardSwarmCallback;
import eu.operando.swarmclient.models.Swarm;
import eu.operando.swarmclient.models.SwarmCallback;

/**
 * Created by Alex on 1/18/2018.
 */

public abstract class SocialNetworkFormBaseActivity extends BaseActivity {

    private ExpandableListView questionsELV;
    private FacebookSettingsListAdapter elvAdapter;
    public static final String PRIVACY_SETTINGS_TAG = "OSP_PRIVACY_SETTINGS";
    private OperandoProgressDialog progressDialog;
    private List<Question> questions;
    private String socialNetwork;

    public String getSocialNetwork() {
        return socialNetwork;
    }

    public void setSocialNetwork(String socialNetwork) {
        this.socialNetwork = socialNetwork;
    }

    public abstract int getContentViewID();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewID());
        initUI();
    }

    private void initUI() {

        initProgressDialog();
        questionsELV = (ExpandableListView) findViewById(R.id.osp_settings_elv);
        questionsELV.setOnGroupExpandListener(new AccordionOnGroupExpandListener(questionsELV));

        setToolbar();
    }

    private void setToolbar() {

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setBackgroundColor(ContextCompat.getColor(getContext(), getSocialNetworkEnum().getColor()));
        myToolbar.setTitle(getSocialNetworkEnum().getToolbarTitle());
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initProgressDialog() {
        progressDialog = new OperandoProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getQuestions();
    }

    public void getQuestions() {

        SwarmClient.getInstance().startSwarm(new PrivacyWizardSwarm("getOSPSettings"), new PrivacyWizardSwarmCallback<PrivacyWizardSwarm>() {

            @Override
            public void call(Swarm result) {

                questions = getQuestionsBySN(((PrivacyWizardSwarm) result).getOspSettings());

                if (questions.size() != 0) {
                    progressDialog.dismiss();
                }

                getUserPreferences();

            }
        });
    }

    private void getUserPreferences() {
        SwarmClient.getInstance().startSwarm(new GetUserPreferencesSwarm(getSocialNetworkEnum().getId()), new SwarmCallback<GetUserPreferencesSwarm>() {

            @Override
            public void call(GetUserPreferencesSwarm result) {
//                        Log.e("GetUserPrefSwm", result.getPreferences().get(0).getSettingValue());
                final Map<Integer, Integer> checkedList;
                if (result.getPreferences().size() == 0) {
                    checkedList = initCheckedStateFromRecommendedValues();
                } else {
                    checkedList = convertAnswersToHashMap(questions, result.getPreferences());
                }
                Log.e("GetUserPrefSwm", String.valueOf(checkedList));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        elvAdapter = new FacebookSettingsListAdapter(getContext(), questions, checkedList, getSocialNetworkEnum());
                        questionsELV.setAdapter(elvAdapter);
                    }
                });
            }
        });
    }

    protected abstract List<Question> getQuestionsBySN(OspSettings ospSettings);

    protected abstract Context getContext();

    public abstract SocialNetworkEnum getSocialNetworkEnum();

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (elvAdapter != null && elvAdapter.getQuestions() != null) {
            if (elvAdapter.getQuestions().size() != 0) {
                sendSaveUserPreferencesSwarm(convertAnswers());
            }
        }
    }

    public void onClickSubmit(View view) {
        List<Preference> userAnswers = convertAnswers();
        sendSaveUserPreferencesSwarm(userAnswers);
        String privacySettings = editOspSettingsJSON(userAnswers);

        Intent intent = new Intent(SocialNetworkFormBaseActivity.this, getWebViewClass());
        intent.putExtra(PRIVACY_SETTINGS_TAG, privacySettings);
        startActivity(intent);
    }

    protected abstract Class getWebViewClass();

    public void onClickRecommended(View view) {
        elvAdapter.initCheckedStateFromRecommendedValues();
        Toast.makeText(this, "Recommended settings loaded", Toast.LENGTH_SHORT).show();
    }

    private String editOspSettingsJSON(List<Preference> userAnswers) {

        JSONArray privacySettings = new JSONArray();
        for (Preference preference : userAnswers) {
            Question question = getQuestionByTag(questions, preference.getSettingKey());
            AvailableSettingsWrite setting = getAvailableSettingsWriteByTag(
                    question, preference.getSettingValue());
            if (setting.getData() != null) {
                question.getWrite().mergeData(setting.getData());
            }
            if (setting.getParams() != null) {
                question.getWrite().setUrl(
                        modifyUrlTemplate(
                                question.getWrite().getUrlTemplate(), setting.getParams()
                        ));
            } else {
                question.getWrite().setUrl(question.getWrite().getUrlTemplate());
            }
            try {
                privacySettings.put(new JSONObject(new Gson().toJson(question.getWrite())));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e("justURL: ", question.getWrite().getUrl());
        }
        Log.e("Privacy OSP Settings", privacySettings.toString());
        return privacySettings.toString();
    }

    private String modifyUrlTemplate(String urlTemplate, List<AvailableSettingsWrite.Param> params) {

        String urlResult = urlTemplate;
        for (AvailableSettingsWrite.Param param : params) {
            if( param.getValue() != null ){
                urlResult = urlResult.replace(
                        new StringBuilder("{").append(param.getPlaceholder()).append("}")
                        , sanitizeParam(param));
            }
        }
        return urlResult;
    }

    private String sanitizeParam(AvailableSettingsWrite.Param param) {
        return param.getValue().toString().replace("\"", "");
    }

    private AvailableSettingsWrite getAvailableSettingsWriteByTag(Question question, String tag) {
        for (int i = 0; i < question.getWrite().getAvailableSettings().size(); ++i) {
            if (question.getWrite().getAvailableSettings().get(i).getTag().equals(tag)) {
                return question.getWrite().getAvailableSettings().get(i);
            }
        }
        return null;
    }

    public void sendSaveUserPreferencesSwarm(List<Preference> userAnswers) {
        SwarmClient.getInstance().startSwarm(new SaveUserPreferencesSwarm(getSocialNetworkEnum().getId(), userAnswers), new SwarmCallback<SaveUserPreferencesSwarm>() {
            @Override
            public void call(SaveUserPreferencesSwarm result) {
                Log.e("SaveUserPreferences", result.toString());
            }
        });
    }

    private List<Preference> convertAnswers() {
        List<Question> questions = elvAdapter.getQuestions();
        Map<Integer, Integer> checkedState = elvAdapter.getCheckedState();
        List<Preference> questionsSettings = new ArrayList<>();
        Log.e("checkedState", String.valueOf(checkedState));


        for (Map.Entry<Integer, Integer> entry : checkedState.entrySet()) {
            Question question = questions.get(entry.getKey());
            String questionTag = question.getTag();
            String questionAnswer = question.getRead().getAvailableSettings().get(entry.getValue())
                    .getTag();
//            Log.e("convertAnswers", questionTag + " " + questionAnswer);
            questionsSettings.add(new Preference(questionTag, questionAnswer));
        }
        return questionsSettings;
    }

    private Map<Integer, Integer> convertAnswersToHashMap(List<Question> questions, List<Preference> preferences) {

        Map<Integer, Integer> checkedList = new TreeMap<>();

        for (int i = 0; i < preferences.size(); ++i) {
            Question question = getQuestionByTag(questions, preferences.get(i).getSettingKey());
            checkedList.put(questions.indexOf(question), getIndexForPreferenceValue(question, preferences.get(i).getSettingValue()));
        }

        return checkedList;
    }

    public Map<Integer, Integer> initCheckedStateFromRecommendedValues() {

        Map<Integer, Integer> checkedState = new TreeMap<>();
        for (int i = 0; i < questions.size(); ++i) {
            List<AvailableSettings> options = questions.get(i).getRead().getAvailableSettings();
            for (int j = 0; j < options.size(); ++j) {
                if (options.get(j).getTag().equals(questions.get(i).getWrite().getRecommended())) {
                    checkedState.put(i, j);
                }
            }
        }
        return checkedState;
    }

    private Question getQuestionByTag(List<Question> questions, String tag) {
        for (int i = 0; i < questions.size(); ++i) {
            if (questions.get(i).getTag().equals(tag)) {
                return questions.get(i);
            }
        }
        return null;
    }

    private int getIndexForPreferenceValue(Question question, String preferenceValue) {
        for (int i = 0; i < question.getRead().getAvailableSettings().size(); ++i) {
            if (preferenceValue.equals(question.getRead().getAvailableSettings().get(i).getTag())) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.facebook_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.scanner_info:
                showInfoDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showInfoDialog() {

        FacebookSettingsInfoDialog dialog = new FacebookSettingsInfoDialog();
        dialog.show(getFragmentManager(), "FacebookSettingsInfoDialog");
    }

}
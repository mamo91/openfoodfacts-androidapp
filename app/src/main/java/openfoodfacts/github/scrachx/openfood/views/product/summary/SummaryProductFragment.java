package openfoodfacts.github.scrachx.openfood.views.product.summary;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import butterknife.BindView;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import openfoodfacts.github.scrachx.openfood.views.LoginActivity;
import openfoodfacts.github.scrachx.openfood.BuildConfig;
import openfoodfacts.github.scrachx.openfood.R;
import openfoodfacts.github.scrachx.openfood.fragments.BaseFragment;
import openfoodfacts.github.scrachx.openfood.models.*;
import openfoodfacts.github.scrachx.openfood.network.OpenFoodAPIClient;
import openfoodfacts.github.scrachx.openfood.network.WikidataApiClient;
import openfoodfacts.github.scrachx.openfood.utils.*;
import openfoodfacts.github.scrachx.openfood.views.*;
import openfoodfacts.github.scrachx.openfood.views.adapters.DialogAddToListAdapter;
import openfoodfacts.github.scrachx.openfood.views.adapters.NutrientLevelListAdapter;
import openfoodfacts.github.scrachx.openfood.views.customtabs.CustomTabActivityHelper;
import openfoodfacts.github.scrachx.openfood.views.customtabs.CustomTabsHelper;
import openfoodfacts.github.scrachx.openfood.views.customtabs.WebViewFallback;
import openfoodfacts.github.scrachx.openfood.views.product.ProductActivity;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.Manifest.permission.*;
import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static openfoodfacts.github.scrachx.openfood.models.ProductImageField.*;
import static openfoodfacts.github.scrachx.openfood.utils.ProductInfoState.EMPTY;
import static openfoodfacts.github.scrachx.openfood.utils.ProductInfoState.LOADING;
import static openfoodfacts.github.scrachx.openfood.utils.Utils.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class SummaryProductFragment extends BaseFragment implements CustomTabActivityHelper.ConnectionCallback, ISummaryProductPresenter.View, ImageUploadListener {
    private static final int LOGIN_ACTIVITY_REQUEST_CODE = 1;
    @BindView(R.id.product_allergen_alert_layout)
    LinearLayout productAllergenAlertLayout;
    @BindView(R.id.product_allergen_alert_text)
    TextView productAllergenAlert;
    @BindView(R.id.textNameProduct)
    TextView nameProduct;
    @BindView(R.id.textQuantityProduct)
    TextView quantityProduct;
    @BindView(R.id.textBrandProduct)
    TextView brandProduct;
    @BindView(R.id.textEmbCode)
    TextView embCode;
    @BindView(R.id.textCategoryProduct)
    TextView categoryProduct;
    @BindView(R.id.textLabelProduct)
    TextView labelProduct;
    @BindView(R.id.front_picture_layout)
    LinearLayout frontPictureLayout;
    @BindView(R.id.imageViewFront)
    ImageView mImageFront;
    @BindView(R.id.addPhotoLabel)
    TextView addPhotoLabel;
    @BindView(R.id.uploadingImageProgress)
    ProgressBar uploadingImageProgress;
    @BindView(R.id.uploadingImageProgressText)
    TextView uploadingImageProgressText;
    @BindView(R.id.buttonMorePictures)
    Button addMorePicture;
    @BindView(R.id.add_nutriscore_prompt)
    Button addNutriScorePrompt;
    @BindView(R.id.imageGrade)
    ImageView nutriscoreImage;
    @BindView(R.id.nova_group)
    ImageView novaGroup;
    @BindView(R.id.co2_icon)
    ImageView co2Icon;
    @BindView(R.id.scores_layout)
    ConstraintLayout scoresLayout;
    @BindView(R.id.listNutrientLevels)
    RecyclerView rv;
    @BindView(R.id.textNutrientTxt)
    TextView textNutrientTxt;
    @BindView(R.id.cvNutritionLights)
    CardView nutritionLightsCardView;
    @BindView(R.id.textAdditiveProduct)
    TextView additiveProduct;
    @BindView(R.id.action_compare_button)
    ImageButton compareProductButton;
    @BindView(R.id.scrollView)
    public NestedScrollView scrollView;
    @BindView(R.id.product_question_layout)
    RelativeLayout productQuestionLayout;
    @BindView(R.id.product_question_text)
    TextView productQuestionText;
    @BindView(R.id.product_question_dismiss)
    ImageView productQuestionDismiss;
    private State state;
    private Product product;
    private OpenFoodAPIClient api;
    private WikidataApiClient apiClientForWikiData;
    private String mUrlImage;
    private String barcode;
    private boolean sendOther = false;
    private CustomTabsIntent customTabsIntent;
    private CustomTabActivityHelper customTabActivityHelper;
    private Uri nutritionScoreUri;
    private TagDao mTagDao;
    private SummaryProductFragment mFragment;
    private ISummaryProductPresenter.Actions presenter;
    //boolean to determine if image should be loaded or not
    private boolean isLowBatteryMode = false;
    //boolean to determine if nutrient prompt should be shown
    private boolean showNutrientPrompt = false;
    //boolean to determine if category prompt should be shown
    private boolean showCategoryPrompt = false;
    //boolean to indicate if the image clicked was that of ingredients
    private boolean addingIngredientsImage = false;
    //boolean to indicate if the image clicked was that of nutrition
    private boolean addingNutritionImage = false;
    private static final int EDIT_REQUEST_CODE = 2;
    private Question productQuestion = null;
    private boolean hasCategoryInsightQuestion = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        customTabActivityHelper = new CustomTabActivityHelper();
        customTabActivityHelper.setConnectionCallback(this);
        customTabsIntent = CustomTabsHelper.getCustomTabsIntent(getContext(), customTabActivityHelper.getSession());

        state=getStateFromActivityIntent();

        presenter = new SummaryProductPresenter(product, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        api = new OpenFoodAPIClient(getActivity());
        apiClientForWikiData = new WikidataApiClient();
        mFragment = this;
        return createView(inflater, container, R.layout.fragment_summary_product);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //done here for android 4 compatibility.
        //a better solution could be to use https://developer.android.com/jetpack/androidx/releases/ but weird issue with it..
        addNutriScorePrompt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_box_blue_18dp,0,0,0);
        addMorePicture.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_a_photo_blue_18dp,0,0,0);
        refreshView(state);
    }

    @Override
    public void refreshView(State state) {
        super.refreshView(state);
        this.state = state;
        product = state.getProduct();
        presenter = new SummaryProductPresenter(product, this);
        categoryProduct.setText(bold(getString(R.string.txtCategories)));
        labelProduct.setText(bold(getString(R.string.txtLabels)));

        //refresh visibility of UI components
        labelProduct.setVisibility(View.VISIBLE);
        brandProduct.setVisibility(View.VISIBLE);
        quantityProduct.setVisibility(View.VISIBLE);
        embCode.setVisibility(View.VISIBLE);
        nameProduct.setVisibility(View.VISIBLE);

        // If Battery Level is low and the user has checked the Disable Image in Preferences , then set isLowBatteryMode to true
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Utils.DISABLE_IMAGE_LOAD = preferences.getBoolean("disableImageLoad", false);
        if (Utils.DISABLE_IMAGE_LOAD && Utils.getBatteryLevel(getContext())) {
            isLowBatteryMode = true;
        }

        //checks the product states_tags to determine which prompt to be shown
        refreshNutriscorePrompt();

        presenter.loadAllergens();
        presenter.loadCategories();
        presenter.loadLabels();
        presenter.loadProductQuestion();
        additiveProduct.setText(bold(getString(R.string.txtAdditives)));
        presenter.loadAdditives();

        mTagDao = Utils.getAppDaoSession(getActivity()).getTagDao();
        barcode = product.getCode();
        String langCode = LocaleHelper.getLanguage(getContext());

        if (isNotBlank(product.getImageUrl())) {
            addPhotoLabel.setVisibility(View.GONE);

            // Load Image if isLowBatteryMode is false
            if (!isLowBatteryMode) {
                Picasso.with(getContext())
                    .load(product.getImageUrl())
                    .into(mImageFront);
            } else {
                mImageFront.setVisibility(View.GONE);
            }

            mUrlImage = product.getImageUrl();
        }

        //TODO use OpenFoodApiService to fetch product by packaging, brands, categories etc

        if (product.getProductName(langCode) != null) {
            nameProduct.setText(product.getProductName(langCode));
        } else {
            nameProduct.setVisibility(View.GONE);
        }

        if (isNotBlank(product.getQuantity())) {
            quantityProduct.setText(product.getQuantity());
        } else {
            quantityProduct.setVisibility(View.GONE);
        }

        if (isNotBlank(product.getBrands())) {
            brandProduct.setClickable(true);
            brandProduct.setMovementMethod(LinkMovementMethod.getInstance());
            brandProduct.setText("");

            String[] brands = product.getBrands().split(",");
            for (int i = 0; i < brands.length; i++) {
                if(i>0){
                    brandProduct.append(", ");
                }
                brandProduct.append(Utils.getClickableText(brands[i].trim(), "", SearchType.BRAND, getActivity(), customTabsIntent));
            }
        } else {
            brandProduct.setVisibility(View.GONE);
        }

        if (product.getEmbTags() != null && !product.getEmbTags().toString().trim().equals("[]")) {
            embCode.setMovementMethod(LinkMovementMethod.getInstance());
            embCode.setText(bold(getString(R.string.txtEMB)));
            embCode.append(" ");

            String[] embTags = product.getEmbTags().toString().replace("[", "").replace("]", "").split(", ");
            for (int i = 0; i < embTags.length; i++) {
                if(i>0){
                    embCode.append(", ");
                }
                String  embTag = embTags[i];
                embCode.append(Utils.getClickableText(getEmbCode(embTag).trim(), getEmbUrl(embTag), SearchType.EMB, getActivity(), customTabsIntent));
            }
        } else {
            embCode.setVisibility(View.GONE);
        }

        // if the device does not have a camera, hide the button
        try {
            if (!Utils.isHardwareCameraInstalled(getContext())) {
                addMorePicture.setVisibility(View.GONE);
            }
        } catch (NullPointerException e) {
            if (BuildConfig.DEBUG) {
                Log.i(getClass().getSimpleName(), e.toString());
            }
        }

        if (BuildConfig.FLAVOR.equals("off")) {
            scoresLayout.setVisibility(View.VISIBLE);
            List<NutrientLevelItem> levelItem = new ArrayList<>();
            Nutriments nutriments = product.getNutriments();

            NutrientLevels nutrientLevels = product.getNutrientLevels();
            NutrimentLevel fat = null;
            NutrimentLevel saturatedFat = null;
            NutrimentLevel sugars = null;
            NutrimentLevel salt = null;
            if (nutrientLevels != null) {
                fat = nutrientLevels.getFat();
                saturatedFat = nutrientLevels.getSaturatedFat();
                sugars = nutrientLevels.getSugars();
                salt = nutrientLevels.getSalt();
            }

            if (!(fat == null && salt == null && saturatedFat == null && sugars == null)) {
                // prefetch the uri
                // currently only available in french translations
                nutritionScoreUri = Uri.parse(getString(R.string.nutriscore_uri));
                customTabActivityHelper.mayLaunchUrl(nutritionScoreUri, null, null);
                Context context = this.getContext();

                if (nutriments != null) {
                    nutritionLightsCardView.setVisibility(View.VISIBLE);
                    Nutriments.Nutriment fatNutriment = nutriments.get(Nutriments.FAT);
                    if (fat != null && fatNutriment != null) {
                        String fatNutrimentLevel = fat.getLocalize(context);
                        levelItem.add(new NutrientLevelItem(getString(R.string.txtFat),
                            fatNutriment.getDisplayStringFor100g(),
                            fatNutrimentLevel,
                            fat.getImageLevel()));
                    }

                    Nutriments.Nutriment saturatedFatNutriment = nutriments.get(Nutriments.SATURATED_FAT);
                    if (saturatedFat != null && saturatedFatNutriment != null) {
                        String saturatedFatLocalize = saturatedFat.getLocalize(context);
                        levelItem.add(new NutrientLevelItem(getString(R.string.txtSaturatedFat), saturatedFatNutriment.getDisplayStringFor100g(),
                            saturatedFatLocalize,
                            saturatedFat.getImageLevel()));
                    }

                    Nutriments.Nutriment sugarsNutriment = nutriments.get(Nutriments.SUGARS);
                    if (sugars != null && sugarsNutriment != null) {
                        String sugarsLocalize = sugars.getLocalize(context);
                        levelItem.add(new NutrientLevelItem(getString(R.string.txtSugars),
                            sugarsNutriment.getDisplayStringFor100g(),
                            sugarsLocalize,
                            sugars.getImageLevel()));
                    }

                    Nutriments.Nutriment saltNutriment = nutriments.get(Nutriments.SALT);
                    if (salt != null && saltNutriment != null) {
                        String saltLocalize = salt.getLocalize(context);
                        levelItem.add(new NutrientLevelItem(getString(R.string.txtSalt),
                            saltNutriment.getDisplayStringFor100g(),
                            saltLocalize,
                            salt.getImageLevel()));
                    }
                }
            } else {
                nutritionLightsCardView.setVisibility(View.GONE);
            }

            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(new NutrientLevelListAdapter(getContext(), levelItem));

            refreshNutriscore();
            refreshNovaIcon();
            refreshCo2Icon();
            refreshScoresLayout();
        } else {
            scoresLayout.setVisibility(View.GONE);
        }
    }

    private void refreshScoresLayout() {
        if (novaGroup.getVisibility() == View.GONE &&
            co2Icon.getVisibility() == View.GONE &&
            nutriscoreImage.getVisibility() == View.GONE &&
            addNutriScorePrompt.getVisibility() == View.GONE) {
            scoresLayout.setVisibility(View.GONE);
        } else {
            scoresLayout.setVisibility(View.VISIBLE);
        }
    }

    private void refreshNutriscore() {
        int nutritionGradeResource =Utils.getImageGrade(product);
        if (nutritionGradeResource!=Utils.NO_DRAWABLE_RESOURCE) {
            nutriscoreImage.setVisibility(View.VISIBLE);
            nutriscoreImage.setImageResource(nutritionGradeResource);
            nutriscoreImage.setOnClickListener(view1 -> {
                CustomTabsIntent customTabsIntent = CustomTabsHelper.getCustomTabsIntent(getContext(), customTabActivityHelper.getSession());
                CustomTabActivityHelper.openCustomTab(SummaryProductFragment.this.getActivity(), customTabsIntent, nutritionScoreUri, new WebViewFallback());
            });
        } else {
            nutriscoreImage.setVisibility(View.GONE);
        }
    }

    private void refreshNovaIcon() {
        if (product.getNovaGroups() != null) {
            novaGroup.setVisibility(View.VISIBLE);
            novaGroup.setImageResource(Utils.getNovaGroupDrawable(product.getNovaGroups()));
            novaGroup.setOnClickListener(view1 -> {
                Uri uri = Uri.parse(getString(R.string.url_nova_groups));
                CustomTabsIntent customTabsIntent = CustomTabsHelper.getCustomTabsIntent(getContext(), customTabActivityHelper.getSession());
                CustomTabActivityHelper.openCustomTab(SummaryProductFragment.this.getActivity(), customTabsIntent, uri, new WebViewFallback());
            });
        } else {
            novaGroup.setVisibility(View.GONE);
            novaGroup.setImageResource(0);
        }
    }

    private void refreshCo2Icon() {
        int environmentImpactResource = Utils.getImageEnvironmentImpact(product);
        if (environmentImpactResource != Utils.NO_DRAWABLE_RESOURCE) {
            co2Icon.setVisibility(View.VISIBLE);
            co2Icon.setImageResource(environmentImpactResource);
        }else{
            co2Icon.setVisibility(View.GONE);
        }
    }

    private void refreshNutriscorePrompt() {
        //checks the product states_tags to determine which prompt to be shown
        List<String> statesTags = product.getStatesTags();
        showCategoryPrompt = (statesTags.contains("en:categories-to-be-completed") &&
            !hasCategoryInsightQuestion);
        Log.e("showCat", String.valueOf(showCategoryPrompt));

        if (product.getNoNutritionData() != null && product.getNoNutritionData().equals("on")) {
            showNutrientPrompt = false;
        } else {
            if (statesTags.contains("en:nutrition-facts-to-be-completed")) {
                showNutrientPrompt = true;
            }
        }

        if (showNutrientPrompt || showCategoryPrompt) {
            addNutriScorePrompt.setVisibility(View.VISIBLE);
            if (showNutrientPrompt && showCategoryPrompt) {
                addNutriScorePrompt.setText(getString(R.string.add_nutrient_category_prompt_text));
            } else if (showNutrientPrompt) {
                addNutriScorePrompt.setText(getString(R.string.add_nutrient_prompt_text));
            } else if (showCategoryPrompt) {
                addNutriScorePrompt.setText(getString(R.string.add_category_prompt_text));
            }
        } else {
            addNutriScorePrompt.setVisibility(View.GONE);
        }
    }

    private CharSequence getAdditiveTag(AdditiveName additive) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                if (additive.getIsWikiDataIdPresent()) {
                    apiClientForWikiData.doSomeThing(additive.getWikiDataId(), (value, result) -> {
                        FragmentActivity activity = getActivity();
                        if (value) {
                            if (activity != null && !activity.isFinishing()) {
                                BottomScreenCommon.showBottomScreen(result, additive,
                                    activity.getSupportFragmentManager());
                            }
                        } else {
                            if (additive.hasOverexposureData()) {
                                if (activity != null && !activity.isFinishing()) {
                                    BottomScreenCommon.showBottomScreen(result, additive,
                                        activity.getSupportFragmentManager());
                                }
                            } else {
                                ProductBrowsingListActivity.startActivity(getContext(), additive.getAdditiveTag(), additive.getName(), SearchType.ADDITIVE);
                            }
                        }
                    });
                } else {
                    FragmentActivity activity = getActivity();
                    if (additive.hasOverexposureData()) {
                        if (activity != null && !activity.isFinishing()) {
                            BottomScreenCommon.showBottomScreen(null, additive,
                                activity.getSupportFragmentManager());
                        }
                    } else {
                        ProductBrowsingListActivity.startActivity(getContext(), additive.getAdditiveTag(), additive.getName(), SearchType.ADDITIVE);
                    }
                }
            }
        };

        spannableStringBuilder.append(additive.getName());
        spannableStringBuilder.setSpan(clickableSpan, 0, spannableStringBuilder.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        // if the additive has an overexposure risk ("high" or "moderate") then append the warning message to it
        if (additive.hasOverexposureData()) {
            boolean isHighRisk = "high".equalsIgnoreCase(additive.getOverexposureRisk());
            Drawable riskIcon;
            String riskWarningStr;
            int riskWarningColor;
            if (isHighRisk) {
                riskIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_additive_high_risk);
                riskWarningStr = getString(R.string.overexposure_high);
                riskWarningColor = getColor(getContext(), R.color.overexposure_high);
            } else {
                riskIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_additive_moderate_risk);
                riskWarningStr = getString(R.string.overexposure_moderate);
                riskWarningColor = getColor(getContext(), R.color.overexposure_moderate);
            }
            riskIcon.setBounds(0, 0, riskIcon.getIntrinsicWidth(), riskIcon.getIntrinsicHeight());
            ImageSpan iconSpan = new ImageSpan(riskIcon, ImageSpan.ALIGN_BOTTOM);

            spannableStringBuilder.append(" - "); // this will be replaced with the risk icon
            spannableStringBuilder.setSpan(iconSpan, spannableStringBuilder.length() - 2, spannableStringBuilder.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

            spannableStringBuilder.append(riskWarningStr);
            spannableStringBuilder
                .setSpan(new ForegroundColorSpan(riskWarningColor), spannableStringBuilder.length() - riskWarningStr.length(), spannableStringBuilder.length(),
                    SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableStringBuilder;
    }

    @Override
    public void showAdditives(List<AdditiveName> additives) {
        additiveProduct.setText(bold(getString(R.string.txtAdditives)));
        additiveProduct.setMovementMethod(LinkMovementMethod.getInstance());
        additiveProduct.append(" ");
        additiveProduct.append("\n");
        additiveProduct.setClickable(true);
        additiveProduct.setMovementMethod(LinkMovementMethod.getInstance());

        for (int i = 0; i < additives.size() - 1; i++) {
            additiveProduct.append(getAdditiveTag(additives.get(i)));
            additiveProduct.append("\n");
        }

        additiveProduct.append(getAdditiveTag((additives.get(additives.size() - 1))));
    }

    @Override
    public void showAdditivesState(String state) {
        getActivity().runOnUiThread(() -> {
            switch (state) {
                case LOADING: {
                    additiveProduct.append(getString(R.string.txtLoading));
                    additiveProduct.setVisibility(View.VISIBLE);
                    break;
                }
                case EMPTY: {
                    additiveProduct.setVisibility(View.GONE);
                    break;
                }
            }
        });
    }

    @Override
    public void showAllergens(List<AllergenName> allergens) {
        if (allergens.isEmpty()) {
            return;
        }

        if (!product.getStatesTags().contains("en:complete")) {
            productAllergenAlert.setText(R.string.product_incomplete_message);
            productAllergenAlertLayout.setVisibility(View.VISIBLE);
            return;
        }

        Set<String> productAllergens = new HashSet<>(product.getAllergensHierarchy());
        productAllergens.addAll(product.getTracesTags());

        List<String> allergenMatch = new ArrayList<>();
        for (AllergenName allergenName : allergens) {
            if (productAllergens.contains(allergenName.getAllergenTag())) {
                allergenMatch.add(allergenName.getName());
            }
        }

        if (allergenMatch.isEmpty()) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(
            String.format("%s\n", getResources().getString(R.string.product_allergen_prompt)));

        boolean first = true;
        for (String allergenName : allergenMatch) {
            String formattedAllergen;
            if (first) {
                formattedAllergen = allergenName;
            } else {
                formattedAllergen = String.format(", %s", allergenName);
            }

            stringBuilder.append(formattedAllergen);
            first = false;
        }
        productAllergenAlert.setText(stringBuilder.toString());
        productAllergenAlertLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void showCategories(List<CategoryName> categories) {
        categoryProduct.setText(bold(getString(R.string.txtCategories)));
        categoryProduct.setMovementMethod(LinkMovementMethod.getInstance());
        categoryProduct.append(" ");
        categoryProduct.setClickable(true);
        categoryProduct.setMovementMethod(LinkMovementMethod.getInstance());

        if (categories.isEmpty()) {
            categoryProduct.setVisibility(View.GONE);
        } else {
            categoryProduct.setVisibility(View.VISIBLE);
            // Add all the categories to text view and link them to wikidata is possible
            for (int i = 0, lastIndex = categories.size() - 1; i <= lastIndex; i++) {
                CategoryName category = categories.get(i);
                CharSequence categoryName = getCategoriesTag(category);
                if (categoryName != null) {
                    // Add category name to text view
                    categoryProduct.append(categoryName);
                    // Add a comma if not the last item
                    if (i != lastIndex) {
                        categoryProduct.append(", ");
                    }
                }
            }
        }
    }

    @Override
    public void showProductQuestion(Question question) {
        if(Utils.isUserLoggedIn(getContext()) && question!=null && !question.isEmpty()){
            productQuestion = question;
            productQuestionText.setText(String.format("%s\n%s",
                question.getQuestion(), question.getValue()));
            productQuestionLayout.setVisibility(View.VISIBLE);
            hasCategoryInsightQuestion = question.getInsightType().equals("category");
        }else{
            productQuestionLayout.setVisibility(View.GONE);
            productQuestion=null;
        }
        refreshNutriscorePrompt();
        refreshScoresLayout();
    }

    @OnClick(R.id.product_question_layout)
    public void onProductQuestionClick() {
        if(productQuestion==null && !Utils.isUserLoggedIn(getContext())){
            return;
        }
        new QuestionDialog(getActivity())
            .setBackgroundColor(R.color.colorPrimaryDark)
            .setQuestion(productQuestion.getQuestion())
            .setValue(productQuestion.getValue())
            .setOnReviewClickListener(new QuestionActionListeners() {
                @Override
                public void onPositiveFeedback(QuestionDialog dialog) {
                    //init POST request
                    sendProductInsights(productQuestion.getInsightId(), 1);
                    dialog.dismiss();
                }

                @Override
                public void onNegativeFeedback(QuestionDialog dialog) {
                    sendProductInsights(productQuestion.getInsightId(), 0);
                    dialog.dismiss();
                }

                @Override
                public void onAmbiguityFeedback(QuestionDialog dialog) {
                    sendProductInsights(productQuestion.getInsightId(), -1);
                    dialog.dismiss();
                }

                @Override
                public void onCancelListener(DialogInterface dialog) {
                    dialog.dismiss();
                }
            })
            .show();
    }

    public void sendProductInsights(String insightId, int annotation) {
        Log.d("SummaryProductFragment",
            String.format("Annotation %d received for insight %s", annotation, insightId));
        presenter.annotateInsight(insightId, annotation);
        productQuestionLayout.setVisibility(View.GONE);
        productQuestion = null;
    }

    public void showAnnotatedInsightToast(InsightAnnotationResponse response) {
        if (response.getStatus().equals("updated") && getActivity()!=null) {
            Toast toast = Toast.makeText(getActivity(), R.string.product_question_submit_message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 500);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @OnClick(R.id.product_question_dismiss)
    public void productQuestionDismiss() {
        productQuestionLayout.setVisibility(View.GONE);
    }

    @Override
    public void showLabels(List<LabelName> labels) {
        labelProduct.setText(bold(getString(R.string.txtLabels)));
        labelProduct.setClickable(true);
        labelProduct.setMovementMethod(LinkMovementMethod.getInstance());
        labelProduct.append(" ");

        for (int i = 0; i < labels.size() - 1; i++) {
            labelProduct.append(getLabelTag(labels.get(i)));
            labelProduct.append(", ");
        }

        labelProduct.append(getLabelTag(labels.get(labels.size() - 1)));
    }

    @Override
    public void showCategoriesState(String state) {
        getActivity().runOnUiThread(() -> {
            switch (state) {
                case LOADING: {
                    categoryProduct.append(getString(R.string.txtLoading));
                    break;
                }
                case EMPTY: {
                    categoryProduct.setVisibility(View.GONE);
                    break;
                }
            }
        });
    }

    @Override
    public void showLabelsState(String state) {
        getActivity().runOnUiThread(() -> {
            switch (state) {
                case LOADING: {
                    labelProduct.append(getString(R.string.txtLoading));
                    break;
                }
                case EMPTY: {
                    labelProduct.setVisibility(View.GONE);
                    break;
                }
            }
        });
    }

    private String getEmbUrl(String embTag) {
        Tag tag = mTagDao.queryBuilder().where(TagDao.Properties.Id.eq(embTag)).unique();
        if (tag != null) {
            return tag.getName();
        }
        return null;
    }

    private String getEmbCode(String embTag) {
        Tag tag = mTagDao.queryBuilder().where(TagDao.Properties.Id.eq(embTag)).unique();
        if (tag != null) {
            return tag.getName();
        }
        return embTag;
    }

    private CharSequence getCategoriesTag(CategoryName category) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                if (category.getIsWikiDataIdPresent()) {
                    apiClientForWikiData.doSomeThing(category.getWikiDataId(), (value, result) -> {
                        if (value) {
                            FragmentActivity activity = getActivity();

                            if (activity != null && !activity.isFinishing()) {
                                BottomScreenCommon.showBottomScreen(result, category,
                                    activity.getSupportFragmentManager());
                            }
                        } else {
                            ProductBrowsingListActivity.startActivity(getContext(),
                                category.getCategoryTag(),
                                category.getName(),
                                SearchType.CATEGORY);
                        }
                    });
                } else {
                    ProductBrowsingListActivity.startActivity(getContext(),
                        category.getCategoryTag(),
                        category.getName(),
                        SearchType.CATEGORY);
                }
            }
        };
        spannableStringBuilder.append(category.getName());
        spannableStringBuilder.setSpan(clickableSpan, 0, spannableStringBuilder.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!category.isNotNull()) {
            StyleSpan iss = new StyleSpan(android.graphics.Typeface.ITALIC); //Span to make text italic
            spannableStringBuilder.setSpan(iss, 0, spannableStringBuilder.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableStringBuilder;
    }

    private CharSequence getLabelTag(LabelName label) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {

                if (label.getIsWikiDataIdPresent()) {
                    apiClientForWikiData.doSomeThing(label.getWikiDataId(), (value, result) -> {
                        if (value) {
                            FragmentActivity activity = getActivity();
                            if (activity != null && !activity.isFinishing()) {
                                BottomScreenCommon.showBottomScreen(result, label,
                                    activity.getSupportFragmentManager());
                            }
                        } else {
                            ProductBrowsingListActivity.startActivity(getContext(),
                                label.getLabelTag(),
                                label.getName(),
                                SearchType.LABEL);
                        }
                    });
                } else {
                    ProductBrowsingListActivity.startActivity(getContext(),
                        label.getLabelTag(),
                        label.getName(),
                        SearchType.LABEL);
                }
            }
        };

        spannableStringBuilder.append(label.getName());

        spannableStringBuilder.setSpan(clickableSpan, 0, spannableStringBuilder.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableStringBuilder;
    }

    @OnClick(R.id.add_nutriscore_prompt)
    public void onAddNutriScorePromptClick() {
        if (BuildConfig.FLAVOR.equals("off")) {
            final SharedPreferences settings = getActivity().getSharedPreferences("login", 0);
            final String login = settings.getString("user", "");
            if (login.isEmpty()) {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.sign_in_to_edit)
                        .positiveText(R.string.txtSignIn)
                        .negativeText(R.string.dialog_cancel)
                        .onPositive((dialog, which) -> {
                            Intent intent = new Intent(getContext(), LoginActivity.class);
                            startActivityForResult(intent, LOGIN_ACTIVITY_REQUEST_CODE);
                            dialog.dismiss();
                        })
                        .onNegative((dialog, which) -> dialog.dismiss())
                        .build().show();
            } else {
                Intent intent = new Intent(getActivity(), AddProductActivity.class);
                intent.putExtra("edit_product", product);
                //adds the information about the prompt when navigating the user to the edit the product
                intent.putExtra("modify_category_prompt", showCategoryPrompt);
                intent.putExtra("modify_nutrition_prompt", showNutrientPrompt);
                startActivity(intent);
            }
        }
    }

    @OnClick(R.id.action_compare_button)
    public void onCompareProductButtonClick() {
        Intent intent = new Intent(getActivity(), ProductComparisonActivity.class);
        intent.putExtra("product_found", true);
        ArrayList<Product> productsToCompare = new ArrayList<>();
        productsToCompare.add(product);
        intent.putExtra("products_to_compare", productsToCompare);
        startActivity(intent);
    }


    @OnClick(R.id.action_share_button)
    public void onShareProductButtonClick() {
        String shareUrl = " " + getString(R.string.website_product) + product.getCode();
        Intent sharingIntent = new Intent();
        sharingIntent.setAction(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = getResources().getString(R.string.msg_share) + shareUrl;
        String shareSub = "\n\n";
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }

    @OnClick(R.id.action_edit_button)
    public void onEditProductButtonClick() {
        final SharedPreferences settings = getActivity().getSharedPreferences("login", 0);
        final String login = settings.getString("user", "");
        if (login.isEmpty()) {
            new MaterialDialog.Builder(getActivity())
                .title(R.string.sign_in_to_edit)
                .positiveText(R.string.txtSignIn)
                .negativeText(R.string.dialog_cancel)
                .onPositive((dialog, which) -> {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivityForResult(intent, LOGIN_ACTIVITY_REQUEST_CODE);
                    dialog.dismiss();
                })
                .onNegative((dialog, which) -> dialog.dismiss())
                .build().show();
        } else {
            Intent intent = new Intent(getActivity(), AddProductActivity.class);
            intent.putExtra("edit_product", product);
            startActivityForResult(intent, EDIT_REQUEST_CODE);
        }
    }

    @OnClick(R.id.action_add_to_list_button)
    public void onBookmarkProductButtonClick() {
        Activity activity = getActivity();

        String barcode = product.getCode();
        String productName = product.getProductName();
        String imageUrl = product.getImageSmallUrl();
        String productDetails = YourListedProducts.getProductBrandsQuantityDetails(product);

        MaterialDialog.Builder addToListBuilder = new MaterialDialog.Builder(activity)
            .title(R.string.add_to_product_lists)
            .customView(R.layout.dialog_add_to_list, true);
        MaterialDialog addToListDialog = addToListBuilder.build();
        addToListDialog.show();
        View addToListView = addToListDialog.getCustomView();
        if (addToListView != null) {
            ProductListsDao productListsDao =ProductListsActivity.getProducListsDaoWithDefaultList(this.getContext());
            List<ProductLists> productLists = productListsDao.loadAll();

            RecyclerView addToListRecyclerView =
                addToListView.findViewById(R.id.rv_dialogAddToList);
            DialogAddToListAdapter addToListAdapter =
                new DialogAddToListAdapter(activity, productLists, barcode, productName, productDetails, imageUrl);
            addToListRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
            addToListRecyclerView.setAdapter(addToListAdapter);
            TextView tvAddToList = addToListView.findViewById(R.id.tvAddToNewList);
            tvAddToList.setOnClickListener(view -> {
                Intent intent = new Intent(activity, ProductListsActivity.class);
                intent.putExtra("product", product);
                activity.startActivity(intent);
            });
        }
    }

    @Override
    public void onCustomTabsConnected() {
        nutriscoreImage.setClickable(true);
    }

    @Override
    public void onCustomTabsDisconnected() {
        nutriscoreImage.setClickable(false);
    }

    @OnClick(R.id.buttonMorePictures)
    public void takeMorePicture() {
        try {
            if (Utils.isHardwareCameraInstalled(getContext())) {
                if (ContextCompat.checkSelfPermission(getActivity(), CAMERA) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                } else {
                    sendOther = true;
                    EasyImage.openCamera(this, 0);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this.getContext(), READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this.getContext(), WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this.getActivity(), new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, Utils.MY_PERMISSIONS_REQUEST_STORAGE);
                } else {
                    sendOther = true;
                    EasyImage.openGallery(this, 0, false);
                }
            }

            if (ContextCompat.checkSelfPermission(this.getContext(), READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getContext(), WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this.getActivity(), READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this.getActivity(), WRITE_EXTERNAL_STORAGE)) {
                    new MaterialDialog.Builder(this.getContext())
                        .title(R.string.action_about)
                        .content(R.string.permission_storage)
                        .neutralText(R.string.txtOk)
                        .onNeutral((dialog, which) -> ActivityCompat
                            .requestPermissions(this.getActivity(), new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, Utils.MY_PERMISSIONS_REQUEST_STORAGE))
                        .show();
                } else {
                    ActivityCompat.requestPermissions(this.getActivity(), new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, Utils.MY_PERMISSIONS_REQUEST_STORAGE);
                }
            }
        } catch (NullPointerException e) {
            Log.i(getClass().getSimpleName(), e.toString());
        }
    }

    @OnClick(R.id.imageViewFront)
    public void openFullScreen(View v) {
        if (mUrlImage != null) {
            Intent intent = new Intent(v.getContext(), FullScreenImage.class);
            Bundle bundle = new Bundle();
            bundle.putString("imageurl", mUrlImage);
            intent.putExtras(bundle);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityOptionsCompat options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(getActivity(), (View) mImageFront,
                        getActivity().getString(R.string.product_transition));
                startActivity(intent, options.toBundle());
            } else {
                startActivity(intent);
            }
        } else {
            // take a picture
            if (ContextCompat.checkSelfPermission(getActivity(), CAMERA) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                sendOther = false;
                if (Utils.isHardwareCameraInstalled(getContext())) {
                    EasyImage.openCamera(this, 0);
                } else {
                    EasyImage.openGallery(getActivity(), 0, false);
                }
            }
        }
    }

    private void onPhotoReturned(File photoFile) {
        ProductImage image = new ProductImage(barcode, FRONT, photoFile);
        image.setFilePath(photoFile.getAbsolutePath());
        api.postImg(getContext(), image, this);
        addPhotoLabel.setVisibility(View.GONE);
        mUrlImage = photoFile.getAbsolutePath();

        Picasso.with(getContext())
            .load(photoFile)
            .fit()
            .into(mImageFront);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                //the booleans are checked to determine if the picture uploaded was due to a prompt click
                //the pictures are uploaded with the correct path
                if (addingIngredientsImage) {
                    ProductImage image = new ProductImage(barcode, INGREDIENTS, new File(resultUri.getPath()));
                    image.setFilePath(resultUri.getPath());
                    showOtherImageProgress();
                    api.postImg(getContext(), image, this);
                    addingIngredientsImage = false;
                }

                if (addingNutritionImage) {
                    ProductImage image = new ProductImage(barcode, NUTRITION, new File(resultUri.getPath()));
                    image.setFilePath(resultUri.getPath());
                    showOtherImageProgress();
                    api.postImg(getContext(), image, this);
                    addingNutritionImage = false;
                }

                if (!sendOther) {
                    onPhotoReturned(new File(resultUri.getPath()));
                } else {
                    ProductImage image = new ProductImage(barcode, OTHER, new File(resultUri.getPath()));
                    image.setFilePath(resultUri.getPath());
                    showOtherImageProgress();
                    api.postImg(getContext(), image, this);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

        EasyImage.handleActivityResult(requestCode, resultCode, data, getActivity(), new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                //Some error handling
            }

            @Override
            public void onImagesPicked(List<File> imageFiles, EasyImage.ImageSource source, int type) {
                CropImage.activity(Uri.fromFile(imageFiles.get(0)))
                    .setCropMenuCropButtonIcon(R.drawable.ic_check_white_24dp)
                    .setAllowFlipping(false)
                    .start(getContext(), mFragment);
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                //Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(getContext());
                    if (photoFile != null) {
                        photoFile.delete();
                    }
                }
            }
        });
        if (requestCode == EDIT_REQUEST_CODE && resultCode == RESULT_OK && data.getBooleanExtra(AddProductActivity.UPLOADED_TO_SERVER, false)) {
            if (getActivity() instanceof ProductActivity) {
                ((ProductActivity) getActivity()).onRefresh();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length <= 0 || grantResults[0] != PERMISSION_GRANTED) {
                    new MaterialDialog.Builder(getActivity())
                        .title(R.string.permission_title)
                        .content(R.string.permission_denied)
                        .negativeText(R.string.txtNo)
                        .positiveText(R.string.txtYes)
                        .onPositive((dialog, which) -> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .show();
                } else {
                    sendOther = false;
                    EasyImage.openCamera(this, 0);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        presenter.dispose();
        super.onDestroyView();
    }

    public void showOtherImageProgress() {
        uploadingImageProgress.setVisibility(View.VISIBLE);
        uploadingImageProgressText.setVisibility(View.VISIBLE);
        uploadingImageProgressText.setText(R.string.toastSending);
    }

    @Override
    public void onSuccess() {
        uploadingImageProgress.setVisibility(View.GONE);
        uploadingImageProgressText.setText(R.string.image_uploaded_successfully);
    }

    @Override
    public void onFailure(String message) {
        uploadingImageProgress.setVisibility(View.GONE);
        uploadingImageProgressText.setVisibility(View.GONE);
        Context context = getContext();
        if(context==null){
            context=OFFApplication.getInstance();
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

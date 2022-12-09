package kz.talipovsn.ebook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.text.Html;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private WebView webView; // Компонент для просмотра html-страниц

    private String resourceDir; // Путь к html-страницам в ресурсах приложения

    private static final String ID_TOPIC = "idTopic"; // Название ключа для хранения ID-кода выбранной темы
    private int idTopic; // ID-код выбранной темы

    static final String CONFIG_FILE_NAME = "Config"; // Имя файла настроек приложения
    private SharedPreferences sPref; // Переменная для работы с настройками программы

    private static final String FULL_SIZE_FONT = "isFullSizeFont"; // Название ключа для хранения выбора крупного шрифта
    private static final String DARK_THEME = "isDarkTheme";
    private static final String MY_SEARCH = "mysearch"; // Название ключа для хранения разрешения поиска
    private boolean isFullSizeFont = true; // Переменная признака выбора крупного шрифта с инициализацией
    private boolean isDarkTheme = false;
    private boolean mysearch = true;// Переменная признака разрешения поиска с инициализацией

    private static final String BOLD_FONT = "isBoldFont";
    private boolean isBoldFont = false;
    private static final String FONT_EXPANSIVA = "isFontExpansiva";
    private boolean isFontExpansiva = false;
    private static final String FONT_VANTAPLAIN = "isFontVantaplain";
    private boolean isFontVantaplain = false;
    private static final String FONT_DEFAULT = "isFontDefault";
    private boolean isFontDefault = true;

    private int currentApiOS; // Переменная для определения версии Android пользователя

    EditText searchText; // Поле для ввода искомого текста
    TextView searchCountText; // Поле для отображения сколько найдено фрагментов поиска
    ImageButton searchForwardButton, searchCloseButton, searchBackButton; // Кнопки навигации поиска
    RelativeLayout searchToolLayout;// Панель поиска
    FloatingActionButton searchButton;// Круглая кнопка поиска

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация переменных для поиска
        searchForwardButton = findViewById(R.id.searchForwardButton);
        searchBackButton = findViewById(R.id.searchBackButton);
        searchCloseButton = findViewById(R.id.searchCloseButton);
        searchText = findViewById(R.id.searchText);
        searchCountText = findViewById(R.id.searchCountText);
        searchButton = findViewById(R.id.searchButton);
        searchToolLayout = findViewById(R.id.searchToolLayout);



        // Инициализация переменной настроек программы
        sPref = getSharedPreferences(CONFIG_FILE_NAME, MODE_PRIVATE);

        // Обработка переворачивания экрана и начальная инициализация выбранной темы (ID_TOPIC) в приложении
        if (savedInstanceState != null) {
            // Вторичное создание окна после переворачивания экрана
            isFullSizeFont = savedInstanceState.getBoolean(FULL_SIZE_FONT, isFullSizeFont);
            isDarkTheme = savedInstanceState.getBoolean(DARK_THEME, isDarkTheme);
            isBoldFont = savedInstanceState.getBoolean(BOLD_FONT, isBoldFont);
            isFontExpansiva = savedInstanceState.getBoolean(FONT_EXPANSIVA, isFontExpansiva);
            isFontVantaplain = savedInstanceState.getBoolean(FONT_VANTAPLAIN, isFontVantaplain);
            isFontDefault = savedInstanceState.getBoolean(FONT_DEFAULT, isFontDefault);
            mysearch = savedInstanceState.getBoolean(MY_SEARCH, mysearch);
            idTopic = savedInstanceState.getInt(ID_TOPIC, R.id.lec01);
        } else {
            // Первый запуск программы до переворачивания экрана
            // Чтение данных с настроек программы
            isFullSizeFont = sPref.getBoolean(FULL_SIZE_FONT, isFullSizeFont);
            mysearch = sPref.getBoolean(MY_SEARCH, mysearch);
            idTopic = sPref.getInt(ID_TOPIC, R.id.lec01);
        }

        // Включение/отключение кнопки поиска в зависимости от настроек пользователя
        if (mysearch) {
            searchButton.setVisibility(View.VISIBLE);
        } else {
            searchButton.setVisibility(View.GONE);
        }

        // Определение API версии Android
        currentApiOS = Build.VERSION.SDK_INT;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Устанавливаем выбранный пункт меню
        try {
            navigationView.setCheckedItem(idTopic);
        } catch (Exception ignore){
        }

        // Поиск компонента для отображения html-страниц
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);

         // ------------- ВАЖНАЯ СЕКЦИЯ ! ---------------------------------
        // Открытие ссылок внутри компонента без вызова внешнего браузера!
        // Чтоб работали такие ссылки в HTML для перехода из страницы в страницу:
        //</div>
        //<p class="msonormal">&nbsp;<a href="/android_asset/HTML/LEC01/lec01.htm">&gt;&gt;&gt; В НАЧАЛО  &gt;&gt;&gt;</a></p>
        //</body>
        //</html>
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (isFontExpansiva) {
                    fontChanger("Expansiva.otf", "mp_font");
                } else if (isFontVantaplain) {
                    fontChanger("Vantaplain.ttf", "mpy_font");
                } else {
                    webView.loadUrl("javascript:document.body.style.setProperty(\"font-family\", \"Times New Roman\");");
                }
            }
        }); // ЭТО ОБЯЗАТЕЛЬНАЯ СТРОКА !!!

        // Патч для HTML чтобы не было глюков! ЭТО ОБЯЗАТЕЛЬНЫЙ КОД !!!
        if (Build.VERSION.SDK_INT >= 24) try {
            Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
            m.invoke(null);
        } catch (Exception ignored) {
        }
        // ---------------------------------------------------------------


        initWebView(isFullSizeFont);

        // Определение пути к html-файлам
        resourceDir = getString(R.string.resource_directory);

        //Инициализация начала просмотра html-страниц
        onNavigationItemSelected(null);

        // Обработчик кнопки Поиск
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchCountText.setText("");
                searchToolLayout.setVisibility(View.VISIBLE);
                searchButton.setVisibility(View.GONE);
                searchText.requestFocus();
            }
        });

        // Обработчик кнопки Поиск Вперед
        searchForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.findNext(true);
            }
        });

        // Обработчик кнопки Поиск Назад
        searchBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.findNext(false);
            }
        });

        // Обработчик нажатий кнопок в окошке поиска
        searchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // Если нажата клавиша Enter
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && ((keyCode == KeyEvent.KEYCODE_ENTER))) {
                    // Скрываем клавиатуру
                    hideSoftInput();
                    // Ищем нужный текст в webView
                    webView.findAll(searchText.getText().toString());
                    // Активируем возможность отображения найденного теккста в webView
                    try {
                        Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);
                        m.invoke(webView, true);
                    } catch (Exception ignored) {
                    }
                }
                return false;
            }
        });

        // Обработчик поиска в WebView
        webView.setFindListener(new WebView.FindListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                searchCountText.setText("");
                if (numberOfMatches > 0) {
                    searchCountText.setText(String.format("%d %s %d", activeMatchOrdinal + 1, getString(R.string.of), numberOfMatches));
                } else {
                    searchCountText.setText(R.string.not_found);
                }
            }
        });

        // Обработчик кнопки закрытия поиска
        searchCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.clearMatches();
                searchText.setText("");
                searchToolLayout.setVisibility(View.GONE);
                if (mysearch) {
                    searchButton.setVisibility(View.VISIBLE);
                }
                hideSoftInput();
            }
        });

        searchCloseButton.performClick();

    }



    // Сохранение данных в буфер при переворачивании экрана
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(FULL_SIZE_FONT, isFullSizeFont); // Сохраняем крупный ли шрифт
        savedInstanceState.putBoolean(DARK_THEME, isDarkTheme);
        savedInstanceState.putBoolean(BOLD_FONT, isBoldFont);
        savedInstanceState.putBoolean(FONT_EXPANSIVA, isFontExpansiva);
        savedInstanceState.putBoolean(FONT_VANTAPLAIN, isFontVantaplain);
        savedInstanceState.putBoolean(FONT_DEFAULT, isFontDefault);
        savedInstanceState.putBoolean(MY_SEARCH, mysearch); // Сохраняем разрешение поиска
        savedInstanceState.putInt(ID_TOPIC, idTopic); // Сохраняем ID текущей темы
        super.onSaveInstanceState(savedInstanceState);
    }

    // Метод при закрытии окна
    @Override
    protected void onStop() {
        super.onStop();
        // Сохранение настроек программы в файл настроек
        SharedPreferences.Editor ed = sPref.edit();
        ed.putBoolean(FULL_SIZE_FONT, isFullSizeFont);
        ed.putBoolean(DARK_THEME, isDarkTheme);
        ed.putBoolean(BOLD_FONT, isBoldFont);
        ed.putBoolean(FONT_EXPANSIVA, isFontExpansiva);
        ed.putBoolean(FONT_VANTAPLAIN, isFontVantaplain);
        ed.putBoolean(FONT_DEFAULT, isFontDefault);
        ed.putBoolean(MY_SEARCH, mysearch);
        ed.putInt(ID_TOPIC, idTopic);
        ed.apply();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Создание меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        // Отключение пункта крупного шрифта для старых версий Android из-за ограничений их WebView
        MenuItem fullSizeItem = menu.findItem(R.id.full_size_font);
        try {
            if (currentApiOS < android.os.Build.VERSION_CODES.LOLLIPOP) {
                isFullSizeFont = true;
                fullSizeItem.setCheckable(false);
                fullSizeItem.setEnabled(false);
                fullSizeItem.setChecked(true);
            } else {
                fullSizeItem.setCheckable(true);
                fullSizeItem.setChecked(isFullSizeFont);
            }
        } catch (Exception ignored) {
        }

        MenuItem darkTheme = menu.findItem(R.id.dark_theme);
        try {
            if (currentApiOS < android.os.Build.VERSION_CODES.LOLLIPOP) {
                isDarkTheme = true;
                darkTheme.setCheckable(false);
                darkTheme.setEnabled(false);
                darkTheme.setChecked(true);
            } else {
                darkTheme.setCheckable(true);
                darkTheme.setChecked(isDarkTheme);
            }
        } catch (Exception ignored) {
        }

        MenuItem menuItemDefault = menu.findItem(R.id.font_default);
        menuItemDefault.setChecked(isFontDefault);

        // Отключение/включение пункта поиска
        MenuItem mySearchItem = menu.findItem(R.id.mysearch);
        try {
            mySearchItem.setChecked(mysearch);
        } catch (Exception ignored) {
        }

        return true;
    }

    // Обработка верхнего правого меню
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.exit:
                finish();
                return true;
            case R.id.full_size_font:
                isFullSizeFont = !item.isChecked();
                item.setChecked(isFullSizeFont);
                initWebView(isFullSizeFont);
                return true;
            case R.id.dark_theme:
                isDarkTheme = !item.isChecked();
                item.setChecked(isDarkTheme);
                initWebViewDark(isDarkTheme);
                return true;
            case R.id.bold_font:
                isBoldFont = !item.isChecked();
                item.setChecked(isBoldFont);
                initWebViewBold(isBoldFont);
                return true;
            case R.id.font_expansiva:
                isFontExpansiva = true;
                isFontVantaplain = false;
                isFontDefault = false;
                item.setChecked(isFontExpansiva);
                initWebViewFonts(FONT_EXPANSIVA);
                return true;
            case R.id.font_vantaplain:
                isFontVantaplain = true;
                isFontDefault = false;
                isFontExpansiva = false;
                item.setChecked(isFontVantaplain);
                initWebViewFonts(FONT_VANTAPLAIN);
                return true;
            case R.id.font_default:
                isFontDefault = true;
                isFontExpansiva = false;
                isFontVantaplain = false;
                item.setChecked(isFontDefault);
                initWebViewFonts(FONT_DEFAULT);
                return true;
            case R.id.mysearch:
                mysearch = !item.isChecked();
                item.setChecked(mysearch);
                if (mysearch) {
                    searchButton.setVisibility(View.VISIBLE);
                } else {
                    searchButton.setVisibility(View.GONE);
                    searchCloseButton.callOnClick();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id;

        if (item == null) { // Вызывается при начальном открытии окна
            id = idTopic;
        } else { // Вызывается при выборе темы из меню тем
            id = item.getItemId();
            // Сохранение выбранной темы, кроме случаев всех внешних переходов
            if (item.isCheckable()) {
                idTopic = id;
            }
        }

        // Блок выбора тем
        if (id == R.id.lec01) {
            webView.loadUrl(resourceDir + "LEC01/lec01.htm");
        }  else if (id == R.id.lec02) {
            webView.loadUrl(resourceDir + "LEC02/lec02.htm");
        } else if (id == R.id.modern) {
            webView.loadUrl(resourceDir + "MODELS/models.htm");

            // Блок внешних переходов
        } else {
            if (id == R.id.nav_view1) { // Переход на внешнюю html-ссылку и внешний браузер
                openLinkExternally(getString(R.string.url_wiki));
            }
            if (id == R.id.nav_view2) { // Переход на внешнюю html-ссылку и внешний браузер
                openLinkExternally(getString(R.string.url_home_tehno));
            }
            if (id == R.id.nav_view3) { // Переход на внешнюю html-ссылку и внешний браузер
                openLinkExternally(getString(R.string.url_katalog));
            }
            if (id == R.id.nav_send) { // Переход на отправку письма автору
                sendMail(getString(R.string.email), getString(R.string.subject), getString(R.string.textmail));
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    // Инициализация компонента просмотра html-страниц
    private void openLinkExternally(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.view)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
        }
    }

    // Посылка письма автору
    private void sendMail(String email, String subject, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(i, getString(R.string.sending_letter)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, R.string.no_installed_email_client, Toast.LENGTH_SHORT).show();
        }
    }

    // Инициализация компонента просмотра html-страниц и размера шрифта
    @SuppressLint("RequiresFeature")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initWebView(boolean isFullScreen) {
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        if ((currentApiOS >= android.os.Build.VERSION_CODES.LOLLIPOP) && (!isFullScreen)) {
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setSupportZoom(true);
        } else {
            webView.getSettings().setLoadWithOverviewMode(false);
            webView.getSettings().setUseWideViewPort(false);
            webView.getSettings().setBuiltInZoomControls(false);
            webView.getSettings().setSupportZoom(false);
        }
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
    }

    @SuppressLint("RequiresFeature")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initWebViewDark(boolean isDarkTheme) {
        webView.getSettings().setDefaultTextEncodingName("utf-8");

        if (!isDarkTheme) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
            }
        }
        else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            }
        }
    }

    @SuppressLint("RequiresFeature")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initWebViewBold(boolean isBoldFont) {
        webView.getSettings().setDefaultTextEncodingName("utf-8");

        if (isBoldFont) {
            webView.loadUrl("javascript:document.body.style.setProperty(\"font-weight\", \"bold\");");
        } else {
            webView.loadUrl("javascript:document.body.style.setProperty(\"font-weight\", \"normal\");");
        }
    }

    @SuppressLint("RequiresFeature")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initWebViewFonts(String isFont) {
        webView.getSettings().setDefaultTextEncodingName("utf-8");

        if (isFont.equals("isFontExpansiva")) {
            fontChanger("Expansiva.otf", "mp_font");
        }
        if (isFont.equals("isFontVantaplain")) {
            fontChanger("Vantaplain.ttf", "mpy_font");
        }
        if (isFont.equals("isFontDefault")) {
            webView.loadUrl("javascript:document.body.style.setProperty(\"font-family\", \"Times New Roman\");");
        }
    }

    public void fontChanger(String font, String family) {
        webView.loadUrl("javascript:(function(){var newStyle=document.createElement('style'); " +
                "newStyle.appendChild(document.createTextNode(\"@font-face{font-family:'"+ family + "'; " +
                "src:url('file:///android_asset/Fonts/" + font + "')}\")); " +
                "document.head.appendChild(newStyle); " +
                "document.body.style.fontFamily = \""+ family + "\";})()");
    }

    // Скрываем клавиатуру
    private void hideSoftInput() {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception ignored) {
        }
    }


}


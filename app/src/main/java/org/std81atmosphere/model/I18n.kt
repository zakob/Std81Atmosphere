package org.std81atmosphere.model

/**
 * User interface strings (Russian and English). Pure Kotlin so it can be
 * tested on the JVM.
 */
object I18n {

    enum class Language(val label: String) {
        RU("Русский"),
        EN("English"),
    }

    private val strings: Map<Language, Map<String, String>> = mapOf(
        Language.RU to mapOf(
            "app_title" to "Стандартная атмосфера",
            "nav_calc" to "Расчёт",
            "nav_table" to "Таблица",
            "nav_plot" to "График",
            "nav_settings" to "Ещё",
            "unit_pa" to "Па",
            "unit_k" to "К",
            "unit_kg_m3" to "кг/м³",
            "unit_kg_mol" to "кг/моль",
            "unit_m3" to "м⁻³",
            "unit_m_s" to "м/с",
            "unit_m" to "м",
            "unit_pa_s" to "Па·с",
            "unit_m2_s" to "м²/с",
            "unit_w_mk" to "Вт/(м·К)",
            "calc_hint" to "Высота, м",
            "calc_empty" to "Введите высоту в метрах",
            "invalid_number" to "Введите высоту числом (метры).",
            "out_of_range" to "Допустимый диапазон:\nот -2000 до 1200000 м.",
            "copied_value" to "Скопировано: {value}",
            "table_from" to "От, м",
            "table_to" to "До, м",
            "table_step" to "Шаг, м",
            "table_build" to "Построить",
            "table_export" to "Копировать CSV",
            "table_h" to "h, м",
            "table_p" to "P, Па",
            "table_t" to "T, К",
            "table_rho" to "ρ, кг/м³",
            "table_empty" to "Таблица пуста — нажмите «Построить»",
            "table_bad_number" to "Введите числа в поля диапазона и шага.",
            "table_bad_step" to "Шаг должен быть больше нуля.",
            "table_bad_order" to "Начало должно быть меньше конца.",
            "table_too_many" to "Слишком много строк — увеличьте шаг.",
            "table_copied" to "CSV скопирован: строк {count}",
            "plot_parameter" to "Параметр",
            "plot_span" to "Диапазон",
            "plot_axis_h" to "h, км",
            "plot_axis_p" to "P, Па",
            "plot_axis_t" to "T, К",
            "plot_axis_rho" to "ρ, кг/м³",
            "plot_axis_a" to "a, м/с",
            "plot_empty" to "Нет данных",
            "settings_title" to "Настройки",
            "settings_language" to "Язык",
            "settings_about" to "О программе",
            "about_title" to "О программе",
            "about_text" to "ГОСТ 4401-81\nСтандартная атмосфера\nпараметры",
            "about_note" to "Расчёт по таблицам стандарта",
            "about_contact" to "Связаться: bad_zako@hotmail.com",
            "close" to "Закрыть",
            "error_generic" to "Не удалось выполнить расчёт.",
        ),
        Language.EN to mapOf(
            "app_title" to "Standard Atmosphere",
            "nav_calc" to "Calc",
            "nav_table" to "Table",
            "nav_plot" to "Plot",
            "nav_settings" to "More",
            "unit_pa" to "Pa",
            "unit_k" to "K",
            "unit_kg_m3" to "kg/m³",
            "unit_kg_mol" to "kg/mol",
            "unit_m3" to "m⁻³",
            "unit_m_s" to "m/s",
            "unit_m" to "m",
            "unit_pa_s" to "Pa·s",
            "unit_m2_s" to "m²/s",
            "unit_w_mk" to "W/(m·K)",
            "calc_hint" to "Height, m",
            "calc_empty" to "Enter height in meters",
            "invalid_number" to "Enter the height as a number (meters).",
            "out_of_range" to "Valid range:\n-2000 to 1200000 m.",
            "copied_value" to "Copied: {value}",
            "table_from" to "From, m",
            "table_to" to "To, m",
            "table_step" to "Step, m",
            "table_build" to "Build",
            "table_export" to "Copy CSV",
            "table_h" to "h, m",
            "table_p" to "P, Pa",
            "table_t" to "T, K",
            "table_rho" to "ρ, kg/m³",
            "table_empty" to "The table is empty — press \"Build\"",
            "table_bad_number" to "Enter numbers for the range and the step.",
            "table_bad_step" to "The step must be greater than zero.",
            "table_bad_order" to "The start must be less than the end.",
            "table_too_many" to "Too many rows — increase the step.",
            "table_copied" to "CSV copied: {count} rows",
            "plot_parameter" to "Parameter",
            "plot_span" to "Range",
            "plot_axis_h" to "h, km",
            "plot_axis_p" to "P, Pa",
            "plot_axis_t" to "T, K",
            "plot_axis_rho" to "ρ, kg/m³",
            "plot_axis_a" to "a, m/s",
            "plot_empty" to "No data",
            "settings_title" to "Settings",
            "settings_language" to "Language",
            "settings_about" to "About",
            "about_title" to "About",
            "about_text" to "GOST 4401-81\nStandard atmosphere\nparameters",
            "about_note" to "Values follow the standard tables",
            "about_contact" to "Contact: bad_zako@hotmail.com",
            "close" to "Close",
            "error_generic" to "The calculation failed.",
        ),
    )

    @Volatile
    var current: Language = Language.RU

    fun tr(key: String, vararg args: Pair<String, Any>): String {
        val template = strings[current]?.get(key) ?: strings[Language.EN]?.get(key) ?: key
        var result = template
        for ((name, value) in args) {
            result = result.replace("{$name}", value.toString())
        }
        return result
    }
}
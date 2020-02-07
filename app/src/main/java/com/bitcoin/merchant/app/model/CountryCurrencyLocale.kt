package com.bitcoin.merchant.app.model

import android.content.Context
import android.util.Log
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.util.AppUtil
import com.google.gson.annotations.SerializedName
import java.util.*
import kotlin.math.min

data class CountryCurrencyLocale(@SerializedName("name") var name: String = "",
                                 @SerializedName("iso") var iso: String = "",
                                 @SerializedName("currency") var currency: String = "",
                                 @SerializedName("lang") var lang: String = "") {
    class CountryCurrencyList : ArrayList<CountryCurrencyLocale>(256)

    val locale: Locale
        get() {
            return Locale.forLanguageTag(lang) ?: Locale.getDefault()
        }

    val image: Int
        get() {
            return countryIsoToImage[iso] ?: R.drawable.iso_us
        }

    companion object {
        const val TAG = "CountryCurrency"
        const val DEFAULT_CURRENCY = "USD"
        const val DEFAULT_COUNTRY = "US"
        const val DEFAULT_LOCALE = "en_US"
        /**
         * It will return an empty string when not found or when currency is unknown.
         */
        fun getFromLocale(context: Context): CountryCurrencyLocale {
            val locale = Locale.getDefault()
            var currencyCode: String? = ""
            var countryIso = ""
            try {
                countryIso = locale.country
                Log.i(TAG, "Currency Locale.country: $countryIso")
                val currency = Currency.getInstance(locale)
                currencyCode = currency.currencyCode
                Log.i(TAG, "Currency Code: " + currencyCode + " for locale: " + locale.displayName)
                Log.i(TAG, "Currency Symbol: " + currency.symbol)
                Log.i(TAG, "Currency Default Fraction Digits: " + currency.defaultFractionDigits)
                if (isCurrencySupported(context, currencyCode) && isCountrySupported(context, countryIso))
                    return get(context, currencyCode!!, countryIso, locale.toLanguageTag())
            } catch (e: Exception) {
                Log.e(TAG, "Currency", e)
                // check if currency can be determined from the country code
                if (countryIso.length >= 2) {
                    return fromCountry(context, countryIso)
                }
            }
            return fromCurrency(context, currencyCode ?: DEFAULT_CURRENCY)
        }

        private fun isCountrySupported(context: Context, isoCode: String?): Boolean {
            getAll(context).forEach {
                if (it.iso == isoCode) return true
            }
            return false
        }

        private fun isCurrencySupported(context: Context, currencyCode: String?): Boolean {
            getAll(context).forEach {
                if (it.currency == currencyCode) return true
            }
            return false
        }

        private fun fromCountry(context: Context, countryIso: String): CountryCurrencyLocale {
            val iso = normalizeCountryIso(countryIso)
            getAll(context).forEach {
                if (it.iso == iso) return it
            }
            return fromCurrency(context, DEFAULT_CURRENCY)
        }

        private fun fromCurrency(context: Context, currencyIso: String): CountryCurrencyLocale {
            val currency = normalizeCurrencyIso(currencyIso)
            getAll(context).forEach {
                if (it.currency == currency) return it
            }
            return get(context, DEFAULT_CURRENCY, DEFAULT_COUNTRY, DEFAULT_LOCALE)
        }

        private fun normalizeCurrencyIso(currency: String): String {
            val iso = currency.trim { it <= ' ' }
            return iso.substring(0, min(3, iso.length)).toUpperCase()
        }

        private fun normalizeCountryIso(countryIso: String): String {
            val iso = countryIso.trim { it <= ' ' }
            return iso.substring(0, min(2, iso.length)).toUpperCase()
        }

        private var ALL: CountryCurrencyList? = null
        private fun loadAll(context: Context): CountryCurrencyList {
            return AppUtil.readFromJsonFile(context, "CountryCurrency.json", CountryCurrencyList::class.java)
        }

        fun getAll(context: Context): CountryCurrencyList {
            if (ALL == null) ALL = loadAll(context)
            return ALL!!
        }

        fun get(context: Context, currency: String, countryIso: String, langLocale: String): CountryCurrencyLocale {
            var cc: CountryCurrencyLocale? = null
            val all = getAll(context)
            all.forEach {
                if (it.iso == countryIso && it.currency == currency) {
                    cc = it
                    if (!langLocale.isEmpty()) {
                        it.lang = langLocale
                    }
                }
            }
            if (cc == null) all.forEach {
                if (it.iso == countryIso) {
                    cc = it
                }
            }
            if (cc == null) all.forEach {
                if (it.currency == currency) {
                    cc = it
                }
            }
            return cc ?: all[0]
        }

        private fun add(iso: String, resource: Int) {
            countryIsoToImage[iso] = resource
        }

        private val countryIsoToImage: MutableMap<String, Int> = HashMap()

        init {
            add("AD", R.drawable.iso_ad)
            add("AE", R.drawable.iso_ae)
            add("AF", R.drawable.iso_af)
            add("AG", R.drawable.iso_ag)
            add("AI", R.drawable.iso_ai)
            add("AL", R.drawable.iso_al)
            add("AM", R.drawable.iso_am)
            add("AN", R.drawable.iso_an)
            add("AO", R.drawable.iso_ao)
            add("AQ", R.drawable.iso_aq)
            add("AR", R.drawable.iso_ar)
            add("AS", R.drawable.iso_as)
            add("AT", R.drawable.iso_at)
            add("AU", R.drawable.iso_au)
            add("AW", R.drawable.iso_aw)
            add("AX", R.drawable.iso_ax)
            add("AZ", R.drawable.iso_az)
            add("BA", R.drawable.iso_ba)
            add("BB", R.drawable.iso_bb)
            add("BD", R.drawable.iso_bd)
            add("BE", R.drawable.iso_be)
            add("BF", R.drawable.iso_bf)
            add("BG", R.drawable.iso_bg)
            add("BH", R.drawable.iso_bh)
            add("BI", R.drawable.iso_bi)
            add("BJ", R.drawable.iso_bj)
            add("BL", R.drawable.iso_bl)
            add("BM", R.drawable.iso_bm)
            add("BN", R.drawable.iso_bn)
            add("BO", R.drawable.iso_bo)
            add("BR", R.drawable.iso_br)
            add("BS", R.drawable.iso_bs)
            add("BT", R.drawable.iso_bt)
            add("BW", R.drawable.iso_bw)
            add("BY", R.drawable.iso_by)
            add("BZ", R.drawable.iso_bz)
            add("CA", R.drawable.iso_ca)
            add("CC", R.drawable.iso_cc)
            add("CD", R.drawable.iso_cd)
            add("CF", R.drawable.iso_cf)
            add("CG", R.drawable.iso_cg)
            add("CH", R.drawable.iso_ch)
            add("CI", R.drawable.iso_ci)
            add("CK", R.drawable.iso_ck)
            add("CL", R.drawable.iso_cl)
            add("CM", R.drawable.iso_cm)
            add("CN", R.drawable.iso_cn)
            add("CO", R.drawable.iso_co)
            add("CR", R.drawable.iso_cr)
            add("CT", R.drawable.iso_ct)
            add("CU", R.drawable.iso_cu)
            add("CV", R.drawable.iso_cv)
            add("CW", R.drawable.iso_cw)
            add("CX", R.drawable.iso_cx)
            add("CY", R.drawable.iso_cy)
            add("CZ", R.drawable.iso_cz)
            add("DE", R.drawable.iso_de)
            add("DJ", R.drawable.iso_dj)
            add("DK", R.drawable.iso_dk)
            add("DM", R.drawable.iso_dm)
            add("DO", R.drawable.iso_do)
            add("DZ", R.drawable.iso_dz)
            add("EC", R.drawable.iso_ec)
            add("EE", R.drawable.iso_ee)
            add("EG", R.drawable.iso_eg)
            add("EH", R.drawable.iso_eh)
            add("ER", R.drawable.iso_er)
            add("ES", R.drawable.iso_es)
            add("ET", R.drawable.iso_et)
            add("EU", R.drawable.iso_eu)
            add("FI", R.drawable.iso_fi)
            add("FJ", R.drawable.iso_fj)
            add("FK", R.drawable.iso_fk)
            add("FM", R.drawable.iso_fm)
            add("FO", R.drawable.iso_fo)
            add("FR", R.drawable.iso_fr)
            add("GA", R.drawable.iso_ga)
            add("GB", R.drawable.iso_gb)
            add("GD", R.drawable.iso_gd)
            add("GE", R.drawable.iso_ge)
            add("GG", R.drawable.iso_gg)
            add("GH", R.drawable.iso_gh)
            add("GI", R.drawable.iso_gi)
            add("GL", R.drawable.iso_gl)
            add("GM", R.drawable.iso_gm)
            add("GN", R.drawable.iso_gn)
            add("GQ", R.drawable.iso_gq)
            add("GR", R.drawable.iso_gr)
            add("GS", R.drawable.iso_gs)
            add("GT", R.drawable.iso_gt)
            add("GU", R.drawable.iso_gu)
            add("GW", R.drawable.iso_gw)
            add("GY", R.drawable.iso_gy)
            add("HK", R.drawable.iso_hk)
            add("HN", R.drawable.iso_hn)
            add("HR", R.drawable.iso_hr)
            add("HT", R.drawable.iso_ht)
            add("HU", R.drawable.iso_hu)
            add("IC", R.drawable.iso_ic)
            add("ID", R.drawable.iso_id)
            add("IE", R.drawable.iso_ie)
            add("IL", R.drawable.iso_il)
            add("IM", R.drawable.iso_im)
            add("IN", R.drawable.iso_in)
            add("IQ", R.drawable.iso_iq)
            add("IR", R.drawable.iso_ir)
            add("IS", R.drawable.iso_is)
            add("IT", R.drawable.iso_it)
            add("JE", R.drawable.iso_je)
            add("JM", R.drawable.iso_jm)
            add("JO", R.drawable.iso_jo)
            add("JP", R.drawable.iso_jp)
            add("KE", R.drawable.iso_ke)
            add("KG", R.drawable.iso_kg)
            add("KH", R.drawable.iso_kh)
            add("KI", R.drawable.iso_ki)
            add("KM", R.drawable.iso_km)
            add("KN", R.drawable.iso_kn)
            add("KP", R.drawable.iso_kp)
            add("KR", R.drawable.iso_kr)
            add("KW", R.drawable.iso_kw)
            add("KY", R.drawable.iso_ky)
            add("KZ", R.drawable.iso_kz)
            add("LA", R.drawable.iso_la)
            add("LB", R.drawable.iso_lb)
            add("LC", R.drawable.iso_lc)
            add("LI", R.drawable.iso_li)
            add("LK", R.drawable.iso_lk)
            add("LR", R.drawable.iso_lr)
            add("LS", R.drawable.iso_ls)
            add("LT", R.drawable.iso_lt)
            add("LU", R.drawable.iso_lu)
            add("LV", R.drawable.iso_lv)
            add("LY", R.drawable.iso_ly)
            add("MA", R.drawable.iso_ma)
            add("MC", R.drawable.iso_mc)
            add("MD", R.drawable.iso_md)
            add("ME", R.drawable.iso_me)
            add("MF", R.drawable.iso_mf)
            add("MG", R.drawable.iso_mg)
            add("MH", R.drawable.iso_mh)
            add("MK", R.drawable.iso_mk)
            add("ML", R.drawable.iso_ml)
            add("MM", R.drawable.iso_mm)
            add("MN", R.drawable.iso_mn)
            add("MO", R.drawable.iso_mo)
            add("MP", R.drawable.iso_mp)
            add("MQ", R.drawable.iso_mq)
            add("MR", R.drawable.iso_mr)
            add("MS", R.drawable.iso_ms)
            add("MT", R.drawable.iso_mt)
            add("MU", R.drawable.iso_mu)
            add("MV", R.drawable.iso_mv)
            add("MW", R.drawable.iso_mw)
            add("MX", R.drawable.iso_mx)
            add("MY", R.drawable.iso_my)
            add("MZ", R.drawable.iso_mz)
            add("NA", R.drawable.iso_na)
            add("NC", R.drawable.iso_nc)
            add("NE", R.drawable.iso_ne)
            add("NF", R.drawable.iso_nf)
            add("NG", R.drawable.iso_ng)
            add("NI", R.drawable.iso_ni)
            add("NL", R.drawable.iso_nl)
            add("NO", R.drawable.iso_no)
            add("NP", R.drawable.iso_np)
            add("NR", R.drawable.iso_nr)
            add("NU", R.drawable.iso_nu)
            add("NZ", R.drawable.iso_nz)
            add("OM", R.drawable.iso_om)
            add("PA", R.drawable.iso_pa)
            add("PE", R.drawable.iso_pe)
            add("PF", R.drawable.iso_pf)
            add("PG", R.drawable.iso_pg)
            add("PH", R.drawable.iso_ph)
            add("PK", R.drawable.iso_pk)
            add("PL", R.drawable.iso_pl)
            add("PN", R.drawable.iso_pn)
            add("PR", R.drawable.iso_pr)
            add("PS", R.drawable.iso_ps)
            add("PT", R.drawable.iso_pt)
            add("PW", R.drawable.iso_pw)
            add("PY", R.drawable.iso_py)
            add("QA", R.drawable.iso_qa)
            add("RE", R.drawable.iso_re)
            add("RO", R.drawable.iso_ro)
            add("RS", R.drawable.iso_rs)
            add("RU", R.drawable.iso_ru)
            add("RW", R.drawable.iso_rw)
            add("SA", R.drawable.iso_sa)
            add("SB", R.drawable.iso_sb)
            add("SC", R.drawable.iso_sc)
            add("SD", R.drawable.iso_sd)
            add("SE", R.drawable.iso_se)
            add("SG", R.drawable.iso_sg)
            add("SH", R.drawable.iso_sh)
            add("SI", R.drawable.iso_si)
            add("SK", R.drawable.iso_sk)
            add("SL", R.drawable.iso_sl)
            add("SM", R.drawable.iso_sm)
            add("SN", R.drawable.iso_sn)
            add("SO", R.drawable.iso_so)
            add("SR", R.drawable.iso_sr)
            add("SS", R.drawable.iso_ss)
            add("ST", R.drawable.iso_st)
            add("SV", R.drawable.iso_sv)
            add("SX", R.drawable.iso_sx)
            add("SY", R.drawable.iso_sy)
            add("SZ", R.drawable.iso_sz)
            add("TC", R.drawable.iso_tc)
            add("TD", R.drawable.iso_td)
            add("TF", R.drawable.iso_tf)
            add("TG", R.drawable.iso_tg)
            add("TH", R.drawable.iso_th)
            add("TJ", R.drawable.iso_tj)
            add("TK", R.drawable.iso_tk)
            add("TL", R.drawable.iso_tl)
            add("TM", R.drawable.iso_tm)
            add("TN", R.drawable.iso_tn)
            add("TO", R.drawable.iso_to)
            add("TR", R.drawable.iso_tr)
            add("TT", R.drawable.iso_tt)
            add("TV", R.drawable.iso_tv)
            add("TW", R.drawable.iso_tw)
            add("TZ", R.drawable.iso_tz)
            add("UA", R.drawable.iso_ua)
            add("UG", R.drawable.iso_ug)
            add("US", R.drawable.iso_us)
            add("UY", R.drawable.iso_uy)
            add("UZ", R.drawable.iso_uz)
            add("VA", R.drawable.iso_va)
            add("VC", R.drawable.iso_vc)
            add("VE", R.drawable.iso_ve)
            add("VG", R.drawable.iso_vg)
            add("VI", R.drawable.iso_vi)
            add("VN", R.drawable.iso_vn)
            add("VU", R.drawable.iso_vu)
            add("WF", R.drawable.iso_wf)
            add("WS", R.drawable.iso_ws)
            add("YE", R.drawable.iso_ye)
            add("YT", R.drawable.iso_yt)
            add("ZA", R.drawable.iso_za)
            add("ZM", R.drawable.iso_zm)
            add("ZW", R.drawable.iso_zw)
        }
    }

    override fun toString(): String {
        return name + "\n" + currency
    }
}

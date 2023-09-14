package com.kiylx.weather.repo

import com.kiylx.libx.http.kotlin.basic2.handleApi2
import com.kiylx.libx.http.kotlin.common.RawResponse
import com.kiylx.libx.http.kotlin.common.Retrofit2Holder
import com.kiylx.weather.common.AllPrefs
import com.kiylx.weather.common.DataCacheUtil
import com.kiylx.weather.repo.api.Api
import com.kiylx.weather.repo.bean.DailyEntity
import com.kiylx.weather.repo.bean.Location
import com.kiylx.weather.repo.bean.Location.Companion.toLatLonStr
import com.kiylx.weather.repo.bean.successData
import com.kiylx.weather.repo.local_file.LocalFile

/**
 * 调用和风天气api，并在这里提供数据
 */
object QWeatherRepo {

    private val api by lazy {
        Retrofit2Holder(AllPrefs.baseUrl).create(Api::class.java)
    }

//<editor-fold desc="网络接口">
    /**
     * 获取实时天气
     * 会查询本地副本
     */
    suspend fun getDailyReport(
        location: Location,
        unit: String = AllPrefs.unit,
        lang: String = AllPrefs.lang,
    ): RawResponse<DailyEntity> {
        val res = if (location.default && AllPrefs.gpsAuto) {
            //默认位置，需要使用经纬度获取数据
            handleApi2(api.getGridDaily(location.toLatLonStr(), lang, unit))
        } else {
            handleApi2(api.getDaily(location.id, lang, unit))
        }
        return res
    }
//</editor-fold>

}
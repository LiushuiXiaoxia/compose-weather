package com.kiylx.weather.ui.page.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kiylx.libx.tools.LocalDateUtil
import com.kiylx.weather.R
import com.kiylx.weather.common.AUnit
import com.kiylx.weather.common.AllPrefs
import com.kiylx.weather.icon.WeatherIcon
import com.kiylx.weather.repo.bean.DayAirEntity
import com.kiylx.weather.repo.bean.DayWeather
import java.time.LocalDate
import java.time.LocalTime
import java.util.function.Function
import java.util.stream.Collectors

/**
 * get multi day weather and show info
 */
@Composable
fun DayWeather(stateHolder: WeatherPagerStateHolder, type: Int = DayWeatherType.threeDayWeather) {
    val dayUiData = when (type) {
        DayWeatherType.threeDayWeather -> {
            stateHolder.threeDayWeatherData
        }

        DayWeatherType.sevenDayWeather -> {
            stateHolder.sevenDayWeatherData
        }

        DayWeatherType.fifteenDayWeather -> {
            stateHolder.fifteenDayWeatherData
        }

        else -> {
            throw IllegalArgumentException("illegal type")
        }
    }
    LaunchedEffect(key1 = Unit) {
        stateHolder.getDayWeatherData(type)
        stateHolder.get5DAir()
    }
    val data: State<DayWeather> = dayUiData.asDataFlow().collectAsState() //未来的每日天气
    val airData: State<DayAirEntity> =
        stateHolder.fiveDayAirData.asDataFlow().collectAsState() //未来5天的空气质量
    val airMap: MutableMap<String, DayAirEntity.Daily> = airData.value.data.stream().collect(
        Collectors.toMap(
            DayAirEntity.Daily::fxDate,
            Function.identity()
        )
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    ) {
        val unit = if (AllPrefs.unit == AUnit.MetricUnits.param) {
            "℃"
        } else {
            "℉"
        }
        val todayLocalDate = LocalDate.now()
        val nowTime = LocalTime.now()

        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(data.value.data.size) {
                val oneDayWeather = data.value.data[it]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(8.dp)
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        val fxDate = LocalDate.parse(oneDayWeather.fxDate)

                        val dateStr = if (fxDate.isEqual(todayLocalDate)) {
                            stringResource(id = R.string.today)
                        } else if (fxDate.minusDays(1).isEqual(todayLocalDate)) {
                            stringResource(id = R.string.tomorrow)
                        } else {
                            fxDate.format(LocalDateUtil.mdFormatter)
                        }
                        //日期拼接上空气质量
                        val dateAndAir = airMap[oneDayWeather.fxDate]?.let {
                            dateStr + " - " + it.category
                        } ?: dateStr
                        Text(dateAndAir)
                        Text(oneDayWeather.textDay + "转" + oneDayWeather.textNight)
                    }
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        //早晚温度
                        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                            Text(text = "${oneDayWeather.tempMax} $unit")
                            Text(text = "${oneDayWeather.tempMin} $unit")
                        }
                        //显示白天还是晚上的天气图标
                        if (nowTime.hour in 6..18) {
                            WeatherIcon(
                                code = oneDayWeather.iconDay.toInt(),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        } else {
                            WeatherIcon(
                                code = oneDayWeather.iconNight.toInt(),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        // todo 未来还可以添加按钮跳转详情页
                    }
                }
            }

        }
    }
}
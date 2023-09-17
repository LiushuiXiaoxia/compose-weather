package com.kiylx.weather.ui.page.main

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.*
import com.kiylx.libx.http.kotlin.basic3.UiState
import com.kiylx.weather.repo.QWeatherGeoRepo
import com.kiylx.weather.ui.activitys.MainViewModel
import com.kiylx.weather.ui.page.UiStateToastMsg
import com.loren.component.view.composesmartrefresh.MyRefreshHeader
import com.loren.component.view.composesmartrefresh.SmartSwipeRefresh
import com.loren.component.view.composesmartrefresh.SmartSwipeStateFlag
import com.loren.component.view.composesmartrefresh.rememberSmartSwipeRefreshState

class MainPage {
    companion object {
        val TAG = "tty1-主页"
    }
}

/**
 * 主页
 */
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MainPage(
    viewModel: MainViewModel,
    navigateToSettings: () -> Unit,
    navigateToLocations: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            IconButton(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopStart),
                onClick = {
                    navigateToLocations()
                }) {
                Icon(Icons.Rounded.AddLocation, contentDescription = "定位")
            }
            IconButton(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd),
                onClick = {
                    navigateToSettings()
                }) {
                Icon(Icons.Rounded.Settings, contentDescription = "设置")
            }
        }
        //根据地点数量显示pager页面
        val allLocations = QWeatherGeoRepo.allLocationsFlow.collectAsState()
        val pagerState = rememberPagerState() {
            allLocations.value.size
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val locationData by remember {
                    mutableStateOf(allLocations.value[it])
                }
                val weatherPagerStateHolder = remember {
                    mutableStateOf(viewModel.getWeatherStateHolder(locationData))
                }
                MainPagePager(weatherPagerStateHolder.value, it)
            }
        }
    }
}

/**
 * 集成下拉刷新，获取天气状况的方法，也就是单个的pager，内部持有真的显示天气状况的页面
 * @param index 0即默认位置，如果开启了gps更新，就得定位刷新
 *
 */
@Composable
fun MainPagePager(weatherPagerStateHolder:WeatherPagerStateHolder, index: Int) {
    //位置信息
    val location=weatherPagerStateHolder.location.value
    //当天的天气状况
    val data = remember { weatherPagerStateHolder.dailyUiState }
    LaunchedEffect(key1 = location, block = {
        weatherPagerStateHolder.getDailyData()
    })
    val pageData = data.asDataFlow().collectAsState()//页面数据

    val uiState = data.asUiStateFlow().collectAsState()
    UiStateToastMsg(state = uiState)//界面状态toast消息

    //下拉刷新
    val refreshState = rememberSmartSwipeRefreshState()
    SmartSwipeRefresh(
        modifier = Modifier.fillMaxSize(),
        onRefresh = {
            weatherPagerStateHolder.getDailyData(true)
        },
        state = refreshState,
        isNeedRefresh = true,
        isNeedLoadMore = false,
        headerIndicator = {
            MyRefreshHeader(refreshState.refreshFlag, true)
        },
    ) {
        //观察界面状态，修改下拉刷新状态
        LaunchedEffect(uiState.value) {
            uiState.value.let {

                when (it) {
                    is UiState.Success<*> -> {
                        refreshState.refreshFlag = SmartSwipeStateFlag.SUCCESS
                    }

                    is UiState.RequestErr,
                    is UiState.OtherErr -> {
                        refreshState.refreshFlag = SmartSwipeStateFlag.ERROR
                    }

                    UiState.Loading -> {}
                    else -> {
                        refreshState.refreshFlag = SmartSwipeStateFlag.IDLE
                    }
                }
            }
        }

            //只有可滑动的时候，下载刷新才能生效，所以，这里配置了垂直滑动
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DailyWeatherHeaderPage(location = location, state = pageData)
                //上面是大概的信息
                //下面是其他数据,每行都是双列
                //tab切换页
                var tabIndex by remember {
                    mutableIntStateOf(0)
                }
                val tabs = listOf("今天", "未来七天")
                TabRow(
                    selectedTabIndex = tabIndex,
                    modifier = Modifier.padding(8.dp),
                    indicator = { tabPositions ->
                        TabRowDefaults.PrimaryIndicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[tabIndex])
                                .height(4.dp),
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, name ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(8.dp)
                                ).heightIn(min=36.dp),
                            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(name)
                        }
                    }
                }
                when (tabIndex) {
                    0 -> {
                        DailyWeatherInfo(weatherPagerStateHolder)
                    }

                    1 -> {
                        DayWeather(weatherPagerStateHolder, DayWeatherType.sevenDayWeather)
                    }

                    2 -> {
                        DayWeather(weatherPagerStateHolder, DayWeatherType.fifteenDayWeather)
                    }
                }
            }

    }
}


class DayWeatherType {
    companion object {
        const val threeDayWeather = 1
        const val sevenDayWeather = 2
        const val fifteenDayWeather = 3
    }
}
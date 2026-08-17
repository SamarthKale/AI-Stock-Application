package com.stockpredictor.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val slides by viewModel.slides.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayColor.Background)
            .padding(ClaySpacing.Lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ClayButton(text = "Skip", onClick = onFinished, variant = ClayButtonVariant.Text)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ClayColor.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(ClaySpacing.Md))
                Text(text = slide.body, color = ClayColor.TextSecondary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = ClaySpacing.Md),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(slides.size) { index ->
                val active = pagerState.currentPage == index
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Spacer(
                        modifier = Modifier
                            .size(if (active) 10.dp else 8.dp)
                            .background(
                                color = if (active) ClayColor.AccentPrimary else ClayColor.TextSecondary.copy(alpha = 0.3f),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
        val isLastPage = slides.isNotEmpty() && pagerState.currentPage == slides.lastIndex
        ClayButton(
            text = if (isLastPage) "Get Started" else "Next",
            onClick = {
                if (isLastPage) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

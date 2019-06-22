/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.calculator2

import android.animation.ArgbEvaluator
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Contains the logic for animating the recyclerview elements when the [HistoryFragment]
 * is dragged down onto the screen.
 */
class DragController {

    // References to views from the Calculator Display.
    /**
     * The [CalculatorFormula] containing the current expression
     */
    private var mDisplayFormula: CalculatorFormula? = null
    /**
     * The [CalculatorResult] containing the current result of our current expression
     */
    private var mDisplayResult: CalculatorResult? = null
    /**
     * The `ToolBar` of the calculator.
     */
    private var mToolbar: View? = null

    /**
     * Translation in the Y direction of the formula whose drag we are animating
     */
    private var mFormulaTranslationY: Int = 0
    /**
     * Translation in the X direction of the formula whose drag we are animating
     */
    private var mFormulaTranslationX: Int = 0
    /**
     * Scale of the formula whose drag we are animating
     */
    private var mFormulaScale: Float = 0f
    /**
     * Scale of the result whose drag we are animating
     */
    private var mResultScale: Float = 0f

    /**
     * Translation in the Y direction of the result whose drag we are animating
     */
    private var mResultTranslationY: Float = 0f
    /**
     * Translation in the X direction of the result whose drag we are animating
     */
    private var mResultTranslationX: Int = 0

    /**
     * Total height of the calculator display -- that is the sum of the heights of the [mToolbar],
     * [mDisplayResult], and [mDisplayFormula]
     */
    private var mDisplayHeight: Int = 0

    /**
     * Starting color for the color change animation of the formula text
     */
    private var mFormulaStartColor: Int = 0
    /**
     * Ending color for the color change animation of the formula text
     */
    private var mFormulaEndColor: Int = 0

    /**
     * Starting color for the color change animation of the result text
     */
    private var mResultStartColor: Int = 0
    /**
     * Ending color for the color change animation of the result text
     */
    private var mResultEndColor: Int = 0

    /**
     * The padding at the bottom of the RecyclerView itself.
     */
    private var mBottomPaddingHeight: Int = 0

    /**
     * Set to *true* after all our animators have been initialized.
     */
    private var mAnimationInitialized: Boolean = false

    /**
     * Are we running on a device which uses a single line display?
     */
    private var mOneLine: Boolean = false
    /**
     * Are both of the formula and result text views empty.
     */
    private var mIsDisplayEmpty: Boolean = false

    /**
     * The [AnimationController] which is animating the calculator display, it is an instance of
     * [EmptyAnimationController] if the display is empty, [ResultAnimationController] if the
     * display contains a result, or just an [AnimationController] if there is something in the
     * formula field (but not necessarily a result).
     */
    private var mAnimationController: AnimationController? = null

    /**
     * The [Evaluator] that the [HistoryFragment] is using (this does not seem to be used by us?)
     */
    private var mEvaluator: Evaluator? = null

    /**
     * Setter for our [mEvaluator] field.
     *
     * @param evaluator the [Evaluator] that the [HistoryFragment] is using
     */
    fun setEvaluator(evaluator: Evaluator) {
        mEvaluator = evaluator
    }

    /**
     * Called from the [HistoryFragment] when it is created (drug down onto the screen) to initialize
     * the animations we are to control, also called from our [initializeAnimation] method to reset
     * all initialized values. First we save our parameter [oneLine] in our field [mOneLine], and
     * our parameter [isDisplayEmpty] in our field [mIsDisplayEmpty]. Then when [mIsDisplayEmpty]
     * is *true* we initialize our field [mAnimationController] with a new instance of
     * [EmptyAnimationController], and if [isResult] is *true* we initialize it with a new instance
     * of [ResultAnimationController]. Otherwise we know that there is something in the formula field
     * (although there may not be a result) so we initialize [mAnimationController] to a new instance
     * of [AnimationController].
     *
     * @param isResult *true* if the display is in the RESULT state.
     * @param oneLine *true* if the device needs to use the one line layout.
     * @param isDisplayEmpty *true* if the calculator display is cleared (no result or formula)
     */
    fun initializeController(isResult: Boolean, oneLine: Boolean, isDisplayEmpty: Boolean) {
        mOneLine = oneLine
        mIsDisplayEmpty = isDisplayEmpty
        when {
            mIsDisplayEmpty -> // Empty display
                mAnimationController = EmptyAnimationController()
            isResult -> // Result
                mAnimationController = ResultAnimationController()
            else -> // There is something in the formula field. There may or may not be
                // a quick result.
                mAnimationController = AnimationController()
        }
    }

    /**
     * Setter for our [mDisplayFormula] field.
     *
     * @param formula the [CalculatorFormula] `AlignedTextView` the calculator is displaying.
     */
    fun setDisplayFormula(formula: CalculatorFormula) {
        mDisplayFormula = formula
    }

    /**
     * Setter for our [mDisplayResult] field.
     *
     * @param result the [CalculatorResult] `AlignedTextView` the calculator is displaying.
     */
    fun setDisplayResult(result: CalculatorResult) {
        mDisplayResult = result
    }

    /**
     * Setter for our [mToolbar] field.
     *
     * @param toolbar the `Toolbar` of the calculator display.
     */
    fun setToolbar(toolbar: View) {
        mToolbar = toolbar
    }

    /**
     * Called to animate the [recyclerView] `RecyclerView` to the state it should be in when
     * [yFraction] of the view is visible. If any of our fields [mDisplayFormula], [mDisplayResult],
     * [mToolbar], or [mEvaluator] are still *null* we have not yet been initialized so we just
     * return having done nothing. Otherwise we initialize our variable `vh` to the `ViewHolder`
     * occupying position 0 in [recyclerView], and if [yFraction] is greater than 0, and `vh` is
     * not *null* we set the visibility of [recyclerView] to VISIBLE. Then if `vh` is not *null*
     * and [mIsDisplayEmpty] is *false* (the calculator is displaying something) and the item view
     * type of `vh` is HISTORY_VIEW_TYPE we want to animate the calculator display contents into
     * [recyclerView] and to do this we:
     * - Initialize our variable `formula` to the `formula` field of `vh`, `result` to the `result`
     * field, `date` to the `date` field and `divider` to the `divider` field.
     * - If our [mAnimationInitialized] field is *false* (this is the first time we have been called
     * to animate [recyclerView] onto the screen) we set our field [mBottomPaddingHeight] to the
     * bottom padding of [recyclerView], call all the appropriate initialization methods of our
     * [mAnimationController] field and set [mAnimationInitialized] to *true*.
     * - We then update all the properties that we are animating for the current value of [yFraction]
     * for each of the views `result`, `formula`, `date` and `divider`.
     *
     * On the otherhand is [mIsDisplayEmpty] is *true* (there is no current expression) we still need
     * to collect information to translate the other `ViewHolder`'s so if [mAnimationInitialized] is
     * *false* we call the `initializeDisplayHeight` method of [mAnimationController] to have it
     * initialize itself for the height of the calculator display and set [mAnimationInitialized] to
     * *true*.
     *
     * Having dealt with the possible animation of the calculator display contents, we now need to
     * move up all the `ViewHolder`'s above the current expression (if there is no current expression,
     * we're translating all the `ViewHolder`'s). To do this we loop over `i` from the last child
     * of [recyclerView] to the lowest index of the first ViewHolder to be translated upwards (which
     * is 1 for both an [ResultAnimationController] and [AnimationController], and 0 for an
     * [EmptyAnimationController]):
     * - We initialize our variable `vh2` to the view holder of the child view at index `i` in
     * [recyclerView]
     * - Then if `vh2` is not *null* we initialize our variable `view` to the `itemView` field of
     * `vh2` and update its `translationY` property to the translation determined for [yFraction]
     * by the `getHistoryElementTranslationY` method of [mAnimationController].
     *
     * @param yFraction Fraction of the dragged [View] that is visible (0.0-1.0) 0.0 is closed.
     * @param recyclerView the [RecyclerView] whose animation we are controlling.
     */
    fun animateViews(yFraction: Float, recyclerView: RecyclerView) {
        if (mDisplayFormula == null
                || mDisplayResult == null
                || mToolbar == null
                || mEvaluator == null) {
            // Bail if we aren't yet initialized.
            return
        }

        val vh = recyclerView.findViewHolderForAdapterPosition(0) as HistoryAdapter.ViewHolder?
        if (yFraction > 0 && vh != null) {
            recyclerView.visibility = View.VISIBLE
        }
        if (vh != null && !mIsDisplayEmpty
                && vh.itemViewType == HistoryAdapter.HISTORY_VIEW_TYPE) {
            val formula = vh.formula
            val result = vh.result
            val date = vh.date
            val divider = vh.divider

            if (!mAnimationInitialized) {
                mBottomPaddingHeight = recyclerView.paddingBottom

                mAnimationController!!.initializeScales(formula, result)

                mAnimationController!!.initializeColorAnimators(formula, result)

                mAnimationController!!.initializeFormulaTranslationX(formula)

                mAnimationController!!.initializeFormulaTranslationY(formula, result)

                mAnimationController!!.initializeResultTranslationX(result)

                mAnimationController!!.initializeResultTranslationY(result)

                mAnimationInitialized = true
            }

            result.scaleX = mAnimationController!!.getResultScale(yFraction)
            result.scaleY = mAnimationController!!.getResultScale(yFraction)

            formula.scaleX = mAnimationController!!.getFormulaScale(yFraction)
            formula.scaleY = mAnimationController!!.getFormulaScale(yFraction)

            formula.pivotX = (formula.width - formula.paddingEnd).toFloat()
            formula.pivotY = (formula.height - formula.paddingBottom).toFloat()

            result.pivotX = (result.width - result.paddingEnd).toFloat()
            result.pivotY = (result.height - result.paddingBottom).toFloat()

            formula.translationX = mAnimationController!!.getFormulaTranslationX(yFraction)
            formula.translationY = mAnimationController!!.getFormulaTranslationY(yFraction)

            result.translationX = mAnimationController!!.getResultTranslationX(yFraction)
            result.translationY = mAnimationController!!.getResultTranslationY(yFraction)

            formula.setTextColor(mColorEvaluator.evaluate(yFraction, mFormulaStartColor,
                    mFormulaEndColor) as Int)

            result.setTextColor(mColorEvaluator.evaluate(yFraction, mResultStartColor,
                    mResultEndColor) as Int)

            date.translationY = mAnimationController!!.getDateTranslationY(yFraction)
            divider.translationY = mAnimationController!!.getDateTranslationY(yFraction)
        } else if (mIsDisplayEmpty) {
            // There is no current expression but we still need to collect information
            // to translate the other ViewHolder's.
            if (!mAnimationInitialized) {
                mAnimationController!!.initializeDisplayHeight()
                mAnimationInitialized = true
            }
        }

        // Move up all ViewHolders above the current expression; if there is no current expression,
        // we're translating all the ViewHolder's.
        for (i in recyclerView.childCount - 1 downTo mAnimationController!!.firstTranslatedViewHolderIndex) {
            val vh2 = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (vh2 != null) {
                val view = vh2.itemView

                view.translationY = mAnimationController!!.getHistoryElementTranslationY(yFraction)
            }
        }
    }

    /**
     * Reset all initialized values, called from the `onStart` override of [HistoryFragment] to
     * initialize us to the state we should be in when the [HistoryFragment] is first visible to
     * the user. We set our field [mAnimationInitialized] to *false* then call our method
     * [initializeController] with our parameters to have it initialize the animations we are to
     * control to their starting state.
     *
     * @param isResult if *true* the calculator is displaying only a result ('=' has been pressed).
     * @param oneLine if *true* the display is just one line.
     * @param isDisplayEmpty if *true* the display is cleared of both formula and result.
     */
    fun initializeAnimation(isResult: Boolean, oneLine: Boolean, isDisplayEmpty: Boolean) {
        mAnimationInitialized = false
        initializeController(isResult, oneLine, isDisplayEmpty)
    }

    /**
     * Interface that each of our classes [AnimationController], [EmptyAnimationController], and
     * [ResultAnimationController] implement so that our [mAnimationController] field can be used to
     * call the methods needed for our animations.
     */
    interface AnimateTextInterface {

        /**
         * Return the lowest index of the first ViewHolder to be translated upwards. If there is no
         * current expression, we translate all the ViewHolder's starting at 0, otherwise we start
         * at index 1 (ie. [AnimationController], and [ResultAnimationController] override this to
         * return 1, and [EmptyAnimationController] overrides it to return 0).
         */
        val firstTranslatedViewHolderIndex: Int

        fun initializeDisplayHeight()

        fun initializeColorAnimators(formula: AlignedTextView, result: CalculatorResult)

        fun initializeScales(formula: AlignedTextView, result: CalculatorResult)

        fun initializeFormulaTranslationX(formula: AlignedTextView)

        fun initializeFormulaTranslationY(formula: AlignedTextView, result: CalculatorResult)

        fun initializeResultTranslationX(result: CalculatorResult)

        fun initializeResultTranslationY(result: CalculatorResult)

        fun getResultTranslationX(yFraction: Float): Float

        fun getResultTranslationY(yFraction: Float): Float

        fun getResultScale(yFraction: Float): Float

        fun getFormulaScale(yFraction: Float): Float

        fun getFormulaTranslationX(yFraction: Float): Float

        fun getFormulaTranslationY(yFraction: Float): Float

        fun getDateTranslationY(yFraction: Float): Float

        fun getHistoryElementTranslationY(yFraction: Float): Float
    }

    // The default AnimationController when Display is in INPUT state and DisplayFormula is not
    // empty. There may or may not be a quick result.
    open inner class AnimationController : AnimateTextInterface {

        override val firstTranslatedViewHolderIndex: Int
            get() = 1

        override fun initializeDisplayHeight() {
            // no-op
        }

        override fun initializeColorAnimators(formula: AlignedTextView, result: CalculatorResult) {
            mFormulaStartColor = mDisplayFormula!!.currentTextColor
            mFormulaEndColor = formula.currentTextColor

            mResultStartColor = mDisplayResult!!.currentTextColor
            mResultEndColor = result.currentTextColor
        }

        override fun initializeScales(formula: AlignedTextView, result: CalculatorResult) {
            // Calculate the scale for the text
            mFormulaScale = mDisplayFormula!!.textSize / formula.textSize
        }

        override fun initializeFormulaTranslationY(formula: AlignedTextView,
                                                   result: CalculatorResult) {
            mFormulaTranslationY = if (mOneLine) {
                // Disregard result since we set it to GONE in the one-line case.
                (mDisplayFormula!!.paddingBottom - formula.paddingBottom
                        - mBottomPaddingHeight)
            } else {
                // Baseline of formula moves by the difference in formula bottom padding and the
                // difference in result height.
                (mDisplayFormula!!.paddingBottom - formula.paddingBottom + mDisplayResult!!.height - result.height
                        - mBottomPaddingHeight)
            }
        }

        override fun initializeFormulaTranslationX(formula: AlignedTextView) {
            // Right border of formula moves by the difference in formula end padding.
            mFormulaTranslationX = mDisplayFormula!!.paddingEnd - formula.paddingEnd
        }

        override fun initializeResultTranslationY(result: CalculatorResult) {
            // Baseline of result moves by the difference in result bottom padding.
            mResultTranslationY = (mDisplayResult!!.paddingBottom - result.paddingBottom
                    - mBottomPaddingHeight).toFloat()
        }

        override fun initializeResultTranslationX(result: CalculatorResult) {
            mResultTranslationX = mDisplayResult!!.paddingEnd - result.paddingEnd
        }

        override fun getResultTranslationX(yFraction: Float): Float {
            return mResultTranslationX * (yFraction - 1f)
        }

        override fun getResultTranslationY(yFraction: Float): Float {
            return mResultTranslationY * (yFraction - 1f)
        }

        override fun getResultScale(yFraction: Float): Float {
            return 1f
        }

        override fun getFormulaScale(yFraction: Float): Float {
            return mFormulaScale + (1f - mFormulaScale) * yFraction
        }

        override fun getFormulaTranslationX(yFraction: Float): Float {
            return mFormulaTranslationX * (yFraction - 1f)
        }

        override fun getFormulaTranslationY(yFraction: Float): Float {
            // Scale linearly between -FormulaTranslationY and 0.
            return mFormulaTranslationY * (yFraction - 1f)
        }

        override fun getDateTranslationY(yFraction: Float): Float {
            // We also want the date to start out above the visible screen with
            // this distance decreasing as it's pulled down.
            // Account for the scaled formula height.
            return -mToolbar!!.height * (1f - yFraction) + getFormulaTranslationY(yFraction) - mDisplayFormula!!.height / getFormulaScale(yFraction) * (1f - yFraction)
        }

        override fun getHistoryElementTranslationY(yFraction: Float): Float {
            return getDateTranslationY(yFraction)
        }
    }

    // The default AnimationController when Display is in RESULT state.
    inner class ResultAnimationController : AnimationController(), AnimateTextInterface {

        override val firstTranslatedViewHolderIndex: Int
            get() = 1

        override fun initializeScales(formula: AlignedTextView, result: CalculatorResult) {
            val textSize = mDisplayResult!!.textSize * mDisplayResult!!.scaleX
            mResultScale = textSize / result.textSize
            mFormulaScale = 1f
        }

        override fun initializeFormulaTranslationY(formula: AlignedTextView, result: CalculatorResult) {
            // Baseline of formula moves by the difference in formula bottom padding and the
            // difference in the result height.
            mFormulaTranslationY = (mDisplayFormula!!.paddingBottom - formula.paddingBottom
                    + mDisplayResult!!.height - result.height - mBottomPaddingHeight)
        }

        override fun initializeFormulaTranslationX(formula: AlignedTextView) {
            // Right border of formula moves by the difference in formula end padding.
            mFormulaTranslationX = mDisplayFormula!!.paddingEnd - formula.paddingEnd
        }

        override fun initializeResultTranslationY(result: CalculatorResult) {
            // Baseline of result moves by the difference in result bottom padding.
            mResultTranslationY = (mDisplayResult!!.paddingBottom.toFloat()
                    - result.paddingBottom.toFloat()
                    - mDisplayResult!!.translationY
                    - mBottomPaddingHeight.toFloat())
        }

        override fun initializeResultTranslationX(result: CalculatorResult) {
            mResultTranslationX = mDisplayResult!!.paddingEnd - result.paddingEnd
        }

        override fun getResultTranslationX(yFraction: Float): Float {
            return mResultTranslationX * yFraction - mResultTranslationX
        }

        override fun getResultTranslationY(yFraction: Float): Float {
            return mResultTranslationY * yFraction - mResultTranslationY
        }

        override fun getFormulaTranslationX(yFraction: Float): Float {
            return mFormulaTranslationX * yFraction - mFormulaTranslationX
        }

        override fun getFormulaTranslationY(yFraction: Float): Float {
            return getDateTranslationY(yFraction)
        }

        override fun getResultScale(yFraction: Float): Float {
            return mResultScale - mResultScale * yFraction + yFraction
        }

        override fun getFormulaScale(yFraction: Float): Float {
            return 1f
        }

        override fun getDateTranslationY(yFraction: Float): Float {
            // We also want the date to start out above the visible screen with
            // this distance decreasing as it's pulled down.
            return (-mToolbar!!.height * (1f - yFraction) + mResultTranslationY * yFraction
                    - mResultTranslationY - mDisplayFormula!!.paddingTop.toFloat()) + mDisplayFormula!!.paddingTop * yFraction
        }
    }

    // The default AnimationController when Display is completely empty.
    inner class EmptyAnimationController : AnimationController(), AnimateTextInterface {

        override val firstTranslatedViewHolderIndex: Int
            get() = 0

        override fun initializeDisplayHeight() {
            mDisplayHeight = (mToolbar!!.height + mDisplayResult!!.height
                    + mDisplayFormula!!.height)
        }

        override fun initializeScales(formula: AlignedTextView, result: CalculatorResult) {
            // no-op
        }

        override fun initializeFormulaTranslationY(formula: AlignedTextView,
                                                   result: CalculatorResult) {
            // no-op
        }

        override fun initializeFormulaTranslationX(formula: AlignedTextView) {
            // no-op
        }

        override fun initializeResultTranslationY(result: CalculatorResult) {
            // no-op
        }

        override fun initializeResultTranslationX(result: CalculatorResult) {
            // no-op
        }

        override fun getResultTranslationX(yFraction: Float): Float {
            return 0f
        }

        override fun getResultTranslationY(yFraction: Float): Float {
            return 0f
        }

        override fun getFormulaScale(yFraction: Float): Float {
            return 1f
        }

        override fun getDateTranslationY(yFraction: Float): Float {
            return 0f
        }

        override fun getHistoryElementTranslationY(yFraction: Float): Float {
            return -mDisplayHeight * (1f - yFraction) - mBottomPaddingHeight
        }
    }

    companion object {

        @Suppress("unused")
        private const val TAG = "DragController"

        private val mColorEvaluator = ArgbEvaluator()
    }
}

package com.example.drawingapp

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.Path
import android.util.TypedValue
import android.view.MotionEvent
import android.util.Log

class DrawingView(context: Context,attrs: AttributeSet) : View(context,attrs)
{
    //drawing path
    private lateinit var drawPath: FingerPath

    //defines what to draw
    private lateinit var canvasPaint: Paint

    //defines how to draw

    private lateinit var drawPaint: Paint
    private var color= Color.BLACK
    private lateinit var canvas: Canvas
    private lateinit var canvusBitmap: Bitmap
    private var brushSize: Float=0.toFloat()
    private  val paths=mutableListOf<FingerPath>()



    init {
        setUpDrawing()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvusBitmap= Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888)
        canvas= Canvas(canvusBitmap)
    }

    // this fun will be called by the system when the user is going to touch the screen
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX=event?.x
        val touchY=event?.y


        when(event?.action)
        {
            MotionEvent.ACTION_DOWN->
            {
                //this happen when user touch the finger on the screen
                drawPath= FingerPath(color,brushSize)
                drawPath.color=color
                drawPath.brushThickness=brushSize.toFloat()


                drawPath.reset()
                drawPath.moveTo(touchX!!,touchY!!)

            }
            //the event will be fired when the user starts to move it;s finger
            //this will fired continuously untill user pickup the finger

            MotionEvent.ACTION_MOVE->
            {
                drawPath.lineTo(touchX!!,touchY!!)
            }

            //when user will pick up finger from screen
            MotionEvent.ACTION_UP->
            {

                paths.add(drawPath)
            }
            else -> {
                return false
            }

        }
        invalidate()//refreshing the layout to reflect the drawing change
        return true
        //return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvusBitmap,0f,0f,drawPaint)
        for (path in paths)
        {
            drawPaint.strokeWidth=path.brushThickness
            drawPaint.color=path.color
            canvas.drawPath(path,drawPaint)
        }

        if(!drawPath.isEmpty)
        {
            drawPaint.strokeWidth=drawPath.brushThickness
            drawPaint.color=drawPath.color
            canvas.drawPath(drawPath,drawPaint)
        }
    }

    private fun setUpDrawing(){
        drawPaint= Paint()
        drawPath= FingerPath(color,brushSize)
        drawPaint.color=color
        drawPaint.style= Paint.Style.STROKE
        drawPaint.strokeJoin= Paint.Join.ROUND
        drawPaint.strokeCap= Paint.Cap.ROUND
        brushSize=20.toFloat()

        canvasPaint= Paint(Paint.DITHER_FLAG)


    }


    fun changeBrushSize(newSize:Float)
    {
        brushSize= TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics
        )
        drawPaint.strokeWidth=brushSize
    }

    fun setColor(newColor: Any?)
    {
        if(newColor is String) {
            color = Color.parseColor(newColor)
            drawPaint.color = color
        }
        else{
            color=newColor as Int
            drawPaint.color=color
        }
    }

    fun undoPath()
    {
        if(paths.size>0)
        {
            Log.d("DrawingView", "Paths size: ${paths.size}")
            paths.removeAt(paths.size-1)
            drawPath.reset()
            invalidate()
        }
    }




    internal inner class FingerPath(var color: Int, var brushThickness: Float): Path()
}



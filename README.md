# StackLayoutManager

一个可以堆叠的LinearLayoutManager

| 动态图                                  | 静态图                                                       |
| --------------------------------------- | ------------------------------------------------------------ |
| ![](files/gif_stack_layout_manager.gif) | ![img_stack_layout_manager.png](https://i.loli.net/2020/09/07/PfSR2nUjCix6sGu.png) |

## 导入依赖

添加`jitpack`仓库

```groovy

```



添加`StackLayoutManager`的依赖

```groovy

```



## 如何使用

直接将StackLayoutManager设置给RecyclerView

```kotlin
recyclerView.layoutManager = StackLayoutManager()
```

## 支持的构造参数

```kotlin
/**
 * @param orientation 支持的方向
 * @param reverseLayout 是否是逆序摆放
 * @param offset item‘间偏移量
 * @param changeDrawingOrder 改变默认绘制顺序
 */
class StackLayoutManager @JvmOverloads constructor(
    private val orientation: Int = HORIZONTAL,
    private val reverseLayout: Boolean = false,
    private val offset: Int = 0,
    private val changeDrawingOrder: Boolean = false
)
```
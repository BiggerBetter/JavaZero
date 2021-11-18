package LeetCode;

import java.util.*;

/**
 * 391. 完美矩形
 * 给你一个数组 rectangles ，其中 rectangles[i] = [xi, yi, ai, bi] 表示一个坐标轴平行的矩形。这个矩形的左下顶点是 (xi, yi) ，右上顶点是 (ai, bi) 。
 * <p>
 * 如果所有矩形一起精确覆盖了某个矩形区域，则返回 true ；否则，返回 false 。
 * <p>
 * 来源：力扣（LeetCode）
 * 链接：https://leetcode-cn.com/problems/perfect-rectangle
 * 著作权归领扣网络所有。商业转载请联系官方授权，非商业转载请注明出处。
 */
public class PerfectRectangle {
    static int[][] rectangles = new int[][]{new int[]{1, 1, 3, 3}, new int[]{3, 1, 4, 2}, new int[]{3, 2, 4, 4}, new int[]{1, 3, 2, 4}, new int[]{2, 3, 3, 4}};

    public static void main(String[] args) {
        System.out.println(isRectangleCover(rectangles));
    }

    public static boolean isRectangleCover(int[][] rectangles) {
        if (!check4Angels(rectangles)) {
            return false;
        }
        //找最左下和最右上对角线的点坐标
        int[] diagonal = findDiagonal(rectangles);

        //计算面积总和，如果对角线的面积和所有小的面积合一样就是对的
        int outerArea = calculateArea(diagonal);
        int sumArea = calculateAreaSum(rectangles);

        return outerArea == sumArea;
    }

    private static int[] findDiagonal(int[][] rectangles) {
        //遍历数组，比较第i个谁大
        int[] base = new int[4];
        for (int i = 0; i < rectangles[0].length; i++) {
            base[i] = rectangles[0][i];
        }
        for (int[] rectangle : rectangles) {
            base[0] = Math.min(base[0], rectangle[0]);
            base[1] = Math.min(base[1], rectangle[1]);
            base[2] = Math.max(base[2], rectangle[2]);
            base[3] = Math.max(base[3], rectangle[3]);
        }
        return base;
    }

    private static int calculateAreaSum(int[][] rectangles) {
        int sum = 0;
        for (int[] rectangle : rectangles) {
            sum += calculateArea(rectangle);
        }
        return sum;
    }

    private static int calculateArea(int[] rectangle) {
        return (rectangle[3] - rectangle[1]) * (rectangle[2] - rectangle[0]);
    }

    private static boolean check4Angels(int[][] rectangles) {
        //把重复的点去掉，如果留下4个的话，而且这四个必须是最外侧的对角顶点，才是没有重叠的
        Set<String> pointSet = new HashSet<>();
        for (int[] rec : rectangles) {
            List<String> linkedPoints = new ArrayList<>(Arrays.asList(get4Points(rec).split("-")));
            linkedPoints.addAll(Arrays.asList(get4Points(findDiagonal(rectangles)).split("-")));
            for (String point : linkedPoints) {
                if (pointSet.contains(point)) {
                    pointSet.remove(point);
                } else {
                    pointSet.add(point);
                }
            }
        }
        return pointSet.size() <= 4;
    }

    private static String get4Points(int[] rectangle) {
        return rectangle[0] + "" + rectangle[1] + "-" +
                rectangle[2] + "" + rectangle[3] + "-" +
                rectangle[0] + "" + rectangle[3] + "-" +
                rectangle[2] + "" + rectangle[1];
    }
}


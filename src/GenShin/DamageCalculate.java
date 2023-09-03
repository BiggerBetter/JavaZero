package GenShin;


/**
 * 元素分四种:
 * 1:增幅反应是增加本来的伤害;
 * 2:剧变反应是新的一套模型,而且出伤也是额外的一个数字;
 * 3:激化反应,本质是增加了基础攻击和倍率的数值的 特殊增幅反应
 * 4:绽放反应,本质是剧变反应.
 * 增幅反应计算:
 * 增幅反应总倍率=反应基础倍率*(1 + 元素精通提升+反应系数提升(比如莫娜一命))
 * 元素精通提升 = (精通*2.78)/(精通+1400)
 * 注意是能上元素的那次伤害才吃这个加成
 * <p>
 * 激化增伤和攻击*倍率一起当一个乘区
 * 激化增伤数值:1664×[1+(5*精通)/(精通+1200)]
 * <p>
 * 绽放倍率为 400
 * 超绽放/烈绽放 倍率基础倍率为 600
 * 乘区还有 精通,等级,抗性
 * 公式:反应基础伤害×( 1+元素精通加成+其他加成)*抗性区
 */

public class DamageCalculate {
    public static void main(String[] args) {

        Lynette lynette = new Lynette();
        lynette.windDamage = 85.6 / 100;
        lynette.attack = 1987;
        double jingtong = 68;//linnite 68 琴72 万叶2330

        //攻击力(生命值,防御力)
        double baseAttack = lynette.attack;
        System.out.println("基础攻击区:" + baseAttack);

        // 技能倍率 琳妮特a第一段79.2% e 536%+62.4%
        double beilvqu = 5.36;
        System.out.println("技能倍率区:" + beilvqu);

        // 增伤
        double zengshang = (85.6 + 20) / 100;
        double zengshangqu = 1 + zengshang;
        System.out.println("增伤区:" + zengshangqu);

        // 双爆区
        double baoshang = 186.0;
        double baoshangqu = 1 + (baoshang / 100);
        System.out.println("爆伤区:" + baoshangqu);

        // 精通加成
        double tiaozhengxishu = 0; //莫娜一命15%
        double zengfujingtongchengqu = 1 + (jingtong * 2.78) / (jingtong + 1400) + tiaozhengxishu;
        double jihuajingtongchengqu = 1 + (jingtong * 5) / (jingtong + 1200) + tiaozhengxishu;
        double jubianjingtongchengqu = 1 + (jingtong * 16) / (jingtong + 2000) + tiaozhengxishu;
        System.out.println("增幅精通区" + zengfujingtongchengqu);
        System.out.println("激化精通区" + jihuajingtongchengqu);
        System.out.println("剧变精通区" + jubianjingtongchengqu);


        //激化增伤区 1664×[1+(5*精通)/(精通+1200)]  //超激化2.3 蔓激化2.5 为啥没用??
        double jihuazengshangqu = 1664 * jihuajingtongchengqu;
        System.out.println("激化增伤区:" + beilvqu);

        // 增幅区
        // 火/冰1.5; 冰/火2; 火/水1.5; 水/火2
        double zengfu = 0;
        double zengfufanyinqu = (zengfu == 0) ? 1 : (zengfu * zengfujingtongchengqu);
        System.out.println("增幅反应乘区:" + zengfufanyinqu);

        // 元素抗性
        // 公子一阶段全是0 普通丘丘人全是10;
        double kangxing = 0;
        double kangxingqu = calKangxing(kangxing);
        System.out.println("抗性乘区:" + kangxingqu);


        //绽放qu 反应基础伤害×(1+元素精通加成+其他加成)*抗性区
        //double zhanfangqu = 400* jingtongchengqu;

        // 防御力
        // 怪物承受伤害= 角色登记+100/(角色登记+100 +(怪物登记+100)*(1-减防系数[比如雷神2命]))
        int fangYuJianShao = 0;
        double fangyuqu = calJianFang(fangYuJianShao);
        System.out.println("防御乘区:" + fangyuqu);

        System.out.println("计算增幅(包括直接直接)伤害为:" +
                baseAttack * beilvqu * zengshangqu * baoshangqu * zengfufanyinqu * kangxingqu * fangyuqu);


        //剧变反应伤害= 基础伤害系数(90级723,80级539)*反应系数*抗性区*精通加成
        //超导1;扩散1.2;感电2.4; 超载4
        System.out.println("计算剧变(扩散)反应伤害为:" + 723 * 1.2 * kangxingqu * jubianjingtongchengqu);

        System.out.println("计算激化伤害为:" +
                (baseAttack * beilvqu + jihuazengshangqu) * zengshangqu * baoshangqu * zengfufanyinqu * kangxingqu * fangyuqu);

//        System.out.println("计算基础绽放伤害为:" +
//                zhanfangqu*zhanfangqu*kangxingqu);


    }

    private static Double calKangxing(double kang) {
        double kangLv = kang / 100;
        if (kangLv >= 0.75) {
            return 1.0 / (1 + kangLv);
        } else if (kangLv >= 0) {
            return 1.0 - kangLv;
        } else {
            return 1.0 - (kangLv / 2);
        }
    }

    private static double calJianFang(int fang) {
        double jianfang = fang / 100; //雷神2命是 60
        int myLevel = 90;
        int enemyLevel = 90;
        return (myLevel + 100) / (myLevel + 100 + (enemyLevel + 100) * (1 - jianfang));
    }
}


/**
 * 90 公子,
 * a1 爆击 2249  不暴击 786
 * 开e后 计算直伤对,
 * e扩散伤害公子 661 计算是 456
 * 将军 1190
 * 女士 1190
 *
 * 万叶 2330 计算是3789.5
 * 神子 精通0 爆伤147.2 雷伤61.6
 * 纳西妲 精通873 爆伤136.3 草伤15
 * 原激化(纳西妲) 爆1213  不暴 513
 * 蔓激化(纳西妲) 豹 不
 * 超激化(神子) 爆5994
 */

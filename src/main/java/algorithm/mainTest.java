package algorithm;

import jxl.Cell;
import jxl.LabelCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @Author HB
 * @Date 2022_04_01
 */
public class mainTest {

    final static Double limitation = Math.cos(Math.PI*5/180);

    static final String[] eleName = new String[]{"Al_2O_3","SiO_2","CaO","Na_2O","P_2O_5","F",
            "Sc","Cr","Zr","Nb","Hf","W","Th","K_2O","TiO_2","MgO","Li","Be","V",
            "Rb","Sr","Cs","Ba","Ta","U","B","Eu","Y","Er","Lu","Ho","Yb","Tm",
            "Dy","La","Ce","Tb","Gd","Pr","Nd","Sm","Cu","Zn","Ga","As","In",
            "Hg","Pb","Bi","Ge","Se","Cd","Sn","Sb","Tl","S_{t,d}","S_{o,d}",
            "Fe_2O_3","MnO","Co","Ni","Mo"};

    public static void main(String[] args) throws IOException, BiffException {
        //
        String root = System.getProperty("user.dir") + "/data/";
        //in the .xls, some missing data has been filled on linear interpolation
        String fileName = "Elemental.xls";
        String filePath = root + fileName;

        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            Workbook workbook = Workbook.getWorkbook(file);
            Sheet sheet = workbook.getSheet(0);
            LabelCell labelCell = sheet.findLabelCell("Pyrite");
            int columnP = labelCell.getColumn();
            //storing the data in the pyrite column
            ArrayList<Double> pyriteValue = new ArrayList<Double>();
            Cell[] pyriteCell = sheet.getColumn(columnP);
            for (int i=1; i<pyriteCell.length; ++i) {
                Double temp = Double.valueOf(pyriteCell[i].getContents());
                pyriteValue.add(temp);
            }
            //storing the column data of each element
            ArrayList<Elements> elementsArr = new ArrayList<Elements>();
            for (int i=0; i<eleName.length; ++i) {
                Elements tempEl = new Elements();
                LabelCell labelCell1 = sheet.findLabelCell(eleName[i]);
                int colNum = labelCell1.getColumn();
                Cell[] tempEleCell = sheet.getColumn(colNum);
                for (int j=1; j<tempEleCell.length; ++j) {
                    Double tempV = Double.valueOf(tempEleCell[j].getContents());
                    tempEl.valueArr.add(tempV);
                }
                elementsArr.add(tempEl);
            }

            /**
             * 获得皮尔逊相关性系数的矩阵
             * get the pearson correlation coefficient matrix
             */
            ArrayList<PearsonLine> pearsonGroup = new ArrayList<PearsonLine>();
            for (int i=0; i<elementsArr.size(); ++i) {
                PearsonLine pearLine = new PearsonLine();
                for (int j=2; j<pyriteValue.size(); ++j) {
                    ArrayList<Double> eleSage = new ArrayList<Double>();
                    ArrayList<Double> pySage = new ArrayList<Double>();
                    for (int k=0; k<=j; ++k) {
                        eleSage.add(elementsArr.get(i).valueArr.get(k));
                        pySage.add(pyriteValue.get(k));
                    }
                    double tempPearson = pearson(eleSage, pySage);
                    pearLine.pearsonLine.add(tempPearson);
                }
                pearsonGroup.add(pearLine);
            }

            /**
             * get the avrage of pearson of perColumn
             */
            ArrayList<Double> avrPearLine = new ArrayList<Double>();
            ArrayList<Double> shortPyLine = new ArrayList<Double>();
            int length = pearsonGroup.get(0).pearsonLine.size();
            for (int i=0; i<length; ++i) {
                shortPyLine.add(pyriteValue.get(i+2));
                double tempValue = 0.0;
                for (int j=0; j<pearsonGroup.size(); ++j) {
                    tempValue += pearsonGroup.get(j).pearsonLine.get(i);
                }
                tempValue /= pearsonGroup.size();
                avrPearLine.add(tempValue);
            }

            /**
             *  求向量以及cosineGroup
             *  get the vector and cosineGroup
             */
            ArrayList<CosineLine> cosineGroup = new ArrayList<CosineLine>();
            for (int i=0; i<pearsonGroup.size(); ++i) {
                CosineLine tempCosLine = new CosineLine();
                for (int j=0; j<shortPyLine.size()-3; ++j) {
                    ArrayList<Double> tempAvrLine = new ArrayList<Double>();
                    ArrayList<Double> tempPearLine = new ArrayList<Double>();
                    ArrayList<Double> tempPyLine = new ArrayList<Double>();
                    for (int k=j; k<shortPyLine.size(); ++k) {
                        tempPearLine.add(pearsonGroup.get(i).pearsonLine.get(k));
                        tempPyLine.add(shortPyLine.get(k));
                        tempAvrLine.add(avrPearLine.get(k));
                    }
                    double tempCos = getCosArr(tempPearLine, tempAvrLine, tempPyLine);
                    tempCosLine.cosineArr.add(tempCos);
                }
                cosineGroup.add(tempCosLine);
            }

            /**
             * 得到avrCosLine
             * get the avrCosLine
             */
            ArrayList<Double> avrCosLine = new ArrayList<Double>();
            for (int i=0; i<cosineGroup.get(0).cosineArr.size(); ++i) {
                double temp = 0.0;
                for (int j=0; j<cosineGroup.size(); ++j) {
                    temp += cosineGroup.get(j).cosineArr.get(i);
                }
                temp /= cosineGroup.size();
                avrCosLine.add(temp);
            }

            System.out.println("avrCosLine printing");
            for (int i=0; i<avrCosLine.size(); ++i) {
                System.out.println("weight percent" + pyriteValue.get(i+2) + ";cosine" + avrCosLine.get(i)
                        + ";corresponding angle" + Math.toDegrees(Math.acos(avrCosLine.get(i))));
            }

            /**
             * 得到寻找的点，并输出对应Pyrite浓度
             * get the poing we are looking for,and print the corresponding weight-percent of Pyrite
             */
            int resultPoint = pointSearch(avrCosLine);
            if (resultPoint >= 0) {
                System.out.println("found the poing and the corresponding wt% of pyrite is: " + pyriteValue.get(resultPoint+2));
            } else {
                System.out.println("the point never exists！");
            }

            //end of if
        }

        System.out.println("sum of string: " + eleName.length);
        //end of main
    }

    /**
     *
     * @param arrEle
     * @param arrAvr
     * @param arrPy
     * @return
     */
    public static double getCosArr(ArrayList<Double> arrEle, ArrayList<Double> arrAvr, ArrayList<Double> arrPy) {
        if (arrEle.size() != arrPy.size()) {
            System.out.println("Inconsistent length！！！");
            return -9999;
        }
        double result = 0.0;
        for (int i=0; i<arrPy.size()-1; ++i) {
            Point eleS = new Point(arrPy.get(i), arrEle.get(i));
            Point eleE = new Point(arrPy.get(i+1), arrEle.get(i+1));
            Line eleLine = new Line(eleS, eleE);

            Point avrS = new Point(arrPy.get(i), arrAvr.get(i));
            Point avrE = new Point(arrPy.get(i+1), arrAvr.get(i+1));
            Line avrLine = new Line(avrS, avrE);

            result += lineCosine(eleLine, avrLine);
        }
        return result/(arrPy.size()-1);
    }

    /**
     *
     * @param l1
     * @param l2
     * @return
     */
    public static double lineCosine(Line l1, Line l2) {
        //
        double x_1 = l1.pEnd.x - l1.pStart.x;
        double y_1 = l1.pEnd.y - l1.pStart.y;

        double x_2 = l2.pEnd.x = l2.pStart.x;
        double y_2 = l2.pEnd.y - l2.pStart.y;

        return (x_1*x_2 + y_1*y_2) / Math.sqrt((x_1*x_1+y_1*y_1) * (x_2*x_2+y_2*y_2));
    }

    /**
     * @description Pearson algorithm
     * @param data1
     * @param data2
     * @return
     */
    public static Double pearson(ArrayList<Double> data1, ArrayList<Double> data2) {
        //
        double avr1 = 0.0;
        double avr2 = 0.0;
        for (Double each : data1) {
            avr1 += each;
        }
        for (Double each : data2) {
            avr2 += each;
        }
        avr1 /= data1.size();
        avr2 /= data2.size();
        double up = 0.0;
        for (int i=0; i<data1.size(); ++i) {
            up += ((data1.get(i)-avr1) * (data2.get(i)-avr2));
        }
        double down1 = 0.0;
        double down2 = 0.0;
        for (int i=0; i<data1.size(); ++i) {
            down1 += Math.pow(data1.get(i)-avr1, 2);
            down2 += Math.pow(data2.get(i)-avr2, 2);
        }

        return up / Math.sqrt(down1 * down2);
    }

    /**
     *
     * @param data
     * @return
     */
    public static int pointSearch(ArrayList<Double> data) {
        //
        for (int i=0; i<data.size(); ++i) {
            if (data.get(i) >= limitation) {
                boolean flag = true;
                for (int j=i; j<data.size(); ++j) {
                    if (data.get(j) < limitation) {
                        flag = !flag;
                        break;
                    }
                }
                if (flag) {
                    return i;
                }
            }
        }
        System.out.println("No such point");
        return -99;
    }

    //end of class
}

class Point {
    double x;
    double y;
    public Point() {}
    public Point(Point sample) {
        this.x = sample.x;
        this.y = sample.y;
    }
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

class Line {
    Point pStart;
    Point pEnd;
    public Line() {}
    public Line(Point s, Point e) {
        this.pStart = new Point(s);
        this.pEnd = new Point(e);
    }
}

class Elements {
    ArrayList<Double> valueArr = new ArrayList<Double>();
}

class PearsonLine {
    ArrayList<Double> pearsonLine = new ArrayList<Double>();
}

class CosineLine {
    ArrayList<Double> cosineArr = new ArrayList<Double>();
}
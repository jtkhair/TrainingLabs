package global.skymind.solution.segmentation;

import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.ColorConversionTransform;
import org.datavec.image.transform.ImageTransform;
import org.datavec.image.transform.PipelineImageTransform;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.bytedeco.opencv.global.opencv_imgproc.CV_RGB2GRAY;

public class Inference {
    private static final Logger log = LoggerFactory.getLogger(Inference.class);
//    private static File modelFilename = new File("segmentUNetFineTune.zip");
private static File modelFilename = new File(System.getProperty("user.home"), ".deeplearning4j/generated-models/segmentUNetFineTune.zip");
    private static final int height = 224;
    private static final int width = 224;
    private static final int channels = 1;
    protected static final long seed = 12345;
    private static final Random random = new Random(seed);
    private static ComputationGraph model;

    public static void main(String[] args) throws Exception {

        if (modelFilename.exists()) {
            log.info("Load model...");
            try {
                model = ModelSerializer.restoreComputationGraph(modelFilename);
            } catch(Exception ex){
                ex.printStackTrace();
            }
        }

        File testImagesPath = new File(System.getProperty("user.home"), ".deeplearning4j/data/data-science-bowl-2018/data-science-bowl-2018/data-science-bowl-2018-2/train/inputs");

        FileSplit imageSplit = new FileSplit(testImagesPath, NativeImageLoader.ALLOWED_FORMATS, random);
//
//        //Load labels
        CustomLabelGenerator labelMaker = new CustomLabelGenerator(height, width, 1); // labels have 1 channel

        // Initialize recordreader
        ImageRecordReader imageRecordReaderTest = new ImageRecordReader(height, width, channels, labelMaker);
        imageRecordReaderTest.initialize(imageSplit, getImageTransform());

        // Dataset iterator
        RecordReaderDataSetIterator imageDataSetTest = new RecordReaderDataSetIterator(imageRecordReaderTest, 1, 1, 1, true);

        // Inference and evaluation on test set
        Evaluation eval = new Evaluation(2);

        while(imageDataSetTest.hasNext())
        {
            DataSet imageSet = imageDataSetTest.next();

            INDArray predict = model.output(imageSet.getFeatures())[0];
            INDArray labels = imageSet.getLabels();

            eval.eval(labels, predict);
            log.info(eval.stats());

            //Intersection over Union:  TP / (TP + FN + FP)
            float IOUNuclei = (float)eval.truePositives().get(1) / ((float)eval.truePositives().get(1) + (float)eval.falsePositives().get(1) + (float)eval.falseNegatives().get(1));
            System.out.println("IOU Cell Nuclei " + String.format("%.3f", IOUNuclei) );

            eval.reset();
        }
    }


    public static ImageTransform getImageTransform() {

//        ImageTransform noise = new NoiseTransform(random, (int) (height * width * 0.1));
//        ImageTransform enhanceContrast = new EqualizeHistTransform();
//        ImageTransform flip = new FlipImageTransform();
        ImageTransform rgb2gray = new ColorConversionTransform(CV_RGB2GRAY);
//        ImageTransform rotate = new RotateImageTransform(random, 30);

        List<Pair<ImageTransform, Double>> pipeline = Arrays.asList(
                new Pair<>(rgb2gray, 1.0)
//                new Pair<>(enhanceContrast, 1.0),
//                new Pair<>(flip, 0.5)
//                new Pair<>(rotate,0.5)
        );
        return new PipelineImageTransform(pipeline, false);
    }
}
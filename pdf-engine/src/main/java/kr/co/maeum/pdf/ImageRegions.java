package kr.co.maeum.pdf;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 이미지가 그려진 변환 좌표를 렌더링된 페이지의 왼쪽 위 원점으로 바꾼다. */
final class ImageRegions extends PDFGraphicsStreamEngine {
    final List<Map<String, Object>> boxes = new ArrayList<>();
    private final PDPage page;
    private final float scale;
    private Point2D current = new Point2D.Float();

    ImageRegions(PDPage page, float scale) {
        super(page);
        this.page = page;
        this.scale = scale;
    }

    @Override
    public void drawImage(PDImage image) {
        if (boxes.size() >= 100) return;
        var matrix = getGraphicsState().getCurrentTransformationMatrix();
        var corners =
                List.of(
                        matrix.transformPoint(0, 0),
                        matrix.transformPoint(1, 0),
                        matrix.transformPoint(0, 1),
                        matrix.transformPoint(1, 1));
        double x0 = Double.MAX_VALUE,
                y0 = Double.MAX_VALUE,
                x1 = -Double.MAX_VALUE,
                y1 = -Double.MAX_VALUE;
        for (var corner : corners) {
            Point2D point = displayPoint(corner);
            x0 = Math.min(x0, point.getX());
            y0 = Math.min(y0, point.getY());
            x1 = Math.max(x1, point.getX());
            y1 = Math.max(y1, point.getY());
        }
        boxes.add(Map.of("bbox", List.of(x0 * scale, y0 * scale, x1 * scale, y1 * scale)));
    }

    private Point2D displayPoint(Point2D point) {
        var box = page.getCropBox();
        double x = point.getX() - box.getLowerLeftX();
        double y = box.getHeight() - (point.getY() - box.getLowerLeftY());
        return switch (Math.floorMod(page.getRotation(), 360)) {
            case 90 -> new Point2D.Double(box.getHeight() - y, x);
            case 180 -> new Point2D.Double(box.getWidth() - x, box.getHeight() - y);
            case 270 -> new Point2D.Double(y, box.getWidth() - x);
            default -> new Point2D.Double(x, y);
        };
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        current = p0;
    }

    @Override
    public void clip(int windingRule) {}

    @Override
    public void moveTo(float x, float y) {
        current = new Point2D.Float(x, y);
    }

    @Override
    public void lineTo(float x, float y) {
        current = new Point2D.Float(x, y);
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        current = new Point2D.Float(x3, y3);
    }

    @Override
    public Point2D getCurrentPoint() {
        return current;
    }

    @Override
    public void closePath() {}

    @Override
    public void endPath() {}

    @Override
    public void strokePath() {}

    @Override
    public void fillPath(int windingRule) {}

    @Override
    public void fillAndStrokePath(int windingRule) {}

    @Override
    public void shadingFill(COSName shadingName) {}
}

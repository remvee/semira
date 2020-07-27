(ns semira.image
  (:import java.awt.Image
           java.awt.image.BufferedImage
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           [javax.imageio IIOImage ImageIO ImageWriteParam]
           javax.imageio.stream.MemoryCacheImageOutputStream))

(defn scale
  "Scale image."
  [image [width height]]
  (.getScaledInstance image width height Image/SCALE_SMOOTH))

(defn crop
  "Crop image."
  [image [x y] [width height]]
  (.getSubimage image x y width height))

(defn dimensions
  "Determine image width and height."
  [image]
  [(.getWidth image) (.getHeight image)])

(defn crop-to-square
  "Crop image to a square.  Center long sides."
  [image]
  (let [[width height] (dimensions image)
        min            (min width height)]
    (crop image
          [(if (> width min) (/ (- width min) 2) 0)
           (if (> height min) (/ (- height min) 2) 0)]
          [min min])))

(defn from-file
  "Read a file into a ImageIO instance."
  [file]
  (ImageIO/read file))

(defn to-stream
  "Create a 80% quality JPEG input stream from given image."
  [image]
  (let [writer       (.next (ImageIO/getImageWritersByFormatName "JPG"))
        bytes-output (ByteArrayOutputStream.)
        image-output (MemoryCacheImageOutputStream. bytes-output)
        result       (BufferedImage. (.getWidth image) (.getHeight image) BufferedImage/TYPE_INT_RGB)]
    (doto (.createGraphics result)
      (.drawImage image nil nil)
      .dispose)
    (doto writer
      (.setOutput image-output)
      (.write nil (IIOImage. result nil nil)
              (doto (.getDefaultWriteParam writer)
                (.setCompressionMode ImageWriteParam/MODE_EXPLICIT)
                (.setCompressionQuality 0.8)))
      .dispose)
    (let [in (ByteArrayInputStream. (.toByteArray bytes-output))]
      (.dispose writer)
      (.flush result)
      (.close bytes-output)
      (.close image-output)
      in)))

package matt.utils

import javax.sound.sampled._
import java.net.URL

class Sound(url: URL) {
  private var stream = AudioSystem.getAudioInputStream(url)
  private var format = stream.getFormat()
  if (format.getEncoding != AudioFormat.Encoding.PCM_SIGNED) {
    format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 
                             format.getSampleRate,
                             format.getSampleSizeInBits * 2,
                             format.getChannels,
                             format.getFrameSize * 2,
                             format.getFrameRate,
                             true)
    stream = AudioSystem.getAudioInputStream(format, stream)
  }
  private var info = new DataLine.Info(classOf[Clip], stream.getFormat, (stream.getFrameLength * format.getFrameSize).intValue)
  private val clip = AudioSystem.getLine(info).asInstanceOf[Clip]
  clip.open(stream)

  def play() {
   if (clip.isRunning)
     clip.stop()
   clip.setFramePosition(0)
   clip.start()
  }
  
}

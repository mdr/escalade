package matt.utils

import java.net.Socket
import java.io.{ObjectInputStream, ObjectOutputStream}

object NetworkUtils {

  def workWithSocketAsClient[Result](socket: Socket)(f: (ObjectInputStream, ObjectOutputStream) => Result) = {
    try {
      val ois = new ObjectInputStream(socket.getInputStream)
      try {
        val oos = new ObjectOutputStream(socket.getOutputStream) 
        try {
          f(ois, oos)
        } finally {
          oos.close()
        }
      } finally {
        ois.close()
      }
    } finally {
      socket.close()
    }
  }
  
  def workWithSocketAsServer[Result](socket: Socket)(f: (ObjectInputStream, ObjectOutputStream) => Result) = {
    try {
      val oos = new ObjectOutputStream(socket.getOutputStream) 
      try {
        val ois = new ObjectInputStream(socket.getInputStream)
        try {
          f(ois, oos)
        } finally {
          ois.close()
        }
      } finally {
        oos.close()
      }
    } finally {
      socket.close()
    }
  }
  
}

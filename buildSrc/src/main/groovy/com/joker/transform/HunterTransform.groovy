package com.joker.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.transforms.ProGuardTransform
import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.annotation.Annotation
import org.apache.commons.io.FileUtils
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.lang.instrument.IllegalClassFormatException
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class HunterTransform extends Transform {
  private static final String CLASS_REGISTRY = 'com.joker.hunter.HunterRegistry'
  private static final String CLASS_REGISTRY_PATH = 'com/joker/hunter/HunterRegistry.class'
  private static final String ANNOTATION_IMPL = 'com.joker.hunter.Impl'
  private static final Logger LOG = Logging.getLogger(HunterTransform.class)

  @Override
  String getName() {
    return "hunterService"
  }

  @Override
  Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS
  }

  @Override
  Set<? super QualifiedContent.Scope> getScopes() {
    return Collections.singleton(QualifiedContent.Scope.SUB_PROJECTS)
  }

  @Override
  boolean isIncremental() {
    return false
  }

  @Override
  void transform(TransformInvocation transformInvocation)
      throws TransformException, InterruptedException, IOException {
    // 1
    transformInvocation.outputProvider.deleteAll()

    def pool = ClassPool.getDefault()

    JarInput registryJarInput
    def impls = []

    // 2
    transformInvocation.inputs.each { input ->

      input.jarInputs.each { JarInput jarInput ->
        pool.appendClassPath(jarInput.file.absolutePath)

        if (new JarFile(jarInput.file).getEntry(CLASS_REGISTRY_PATH) != null) {
          registryJarInput = jarInput
          LOG.info("registryJarInput.file.path is ${registryJarInput.file.absolutePath}")
        } else {
          def jarFile = new JarFile(jarInput.file)
          jarFile.entries().grep { entry -> entry.name.endsWith(".class") }.each { entry ->
            InputStream stream = jarFile.getInputStream(entry)
            if (stream != null) {
              CtClass ctClass = pool.makeClass(stream)
              if (ctClass.hasAnnotation(ANNOTATION_IMPL)) {
                impls.add(ctClass)
              }
              ctClass.detach()
            }
          }

          FileUtils.copyFile(jarInput.file,
              transformInvocation.outputProvider.getContentLocation(jarInput.name,
                  jarInput.contentTypes, jarInput.scopes, Format.JAR))
          LOG.info("jarInput.file.path is $jarInput.file.absolutePath")
        }
      }
    }
    if (registryJarInput == null) {
      return
    }

    // 3
    def stringBuilder = new StringBuilder()
    stringBuilder.append('{\n')
    stringBuilder.append('services = new java.util.HashMap();')
    impls.each { CtClass ctClass ->
      ClassFile classFile = ctClass.getClassFile()
      AnnotationsAttribute attr = (AnnotationsAttribute) classFile.getAttribute(
          AnnotationsAttribute.invisibleTag)
      Annotation annotation = attr.getAnnotation(ANNOTATION_IMPL)
      def value = annotation.getMemberValue('service')
      stringBuilder.append('services.put(')
          .append(value)
          .append(', new ')
          .append(ctClass.name)
          .append('());\n')
    }
    stringBuilder.append('}\n')
    LOG.info(stringBuilder.toString())

    def registryClz = pool.get(CLASS_REGISTRY)
    registryClz.makeClassInitializer().setBody(stringBuilder.toString())

    // 4
    def outDir = transformInvocation.outputProvider.getContentLocation(registryJarInput.name,
        registryJarInput.contentTypes, registryJarInput.scopes, Format.JAR)

    copyJar(registryJarInput.file, outDir, CLASS_REGISTRY_PATH, registryClz.toBytecode())
  }

  private void copyJar(File srcFile, File outDir, String fileName, byte[] bytes) {
    outDir.getParentFile().mkdirs()

    def jarOutputStream = new JarOutputStream(new FileOutputStream(outDir))
    def buffer = new byte[1024]
    int read = 0

    def jarFile = new JarFile(srcFile)
    jarFile.entries().each { JarEntry jarEntry ->
      if (jarEntry.name == fileName) {
        jarOutputStream.putNextEntry(new JarEntry(fileName))
        jarOutputStream.write(bytes)
      } else {
        jarOutputStream.putNextEntry(jarEntry)
        def inputStream = jarFile.getInputStream(jarEntry)
        while ((read = inputStream.read(buffer)) != -1) {
          jarOutputStream.write(buffer, 0, read)
        }
      }
    }
    jarOutputStream.close()
  }
}

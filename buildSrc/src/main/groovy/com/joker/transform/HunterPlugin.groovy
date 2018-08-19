package com.joker.transform

import org.gradle.api.Plugin
import org.gradle.api.Project

class HunterPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.plugins.withId('com.android.application') {
      project.android.registerTransform(new HunterTransform())
    }
  }
}

/**
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zalando.maven.plugins.swagger.codegen;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.zalando.stups.swagger.codegen.CodegenerationException;
import org.zalando.stups.swagger.codegen.StandaloneCodegenerator;
import org.zalando.stups.swagger.codegen.YamlToJson;

/**
 * @author  jbellmann
 */
@Mojo(
    name = "codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true, threadSafe = false,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/swagger-codegen")
    private File outputDirectory;

    @Parameter(required = true, defaultValue = "${project.basedir}/src/main/resources/swagger.yaml")
    private String apiFile;

    @Parameter(required = true)
    private String language;

    @Parameter
    private String apiPackage;

    @Parameter
    private String modelPackage;

    @Parameter(defaultValue = "false")
    private boolean skipModelgeneration = false;

    @Parameter
    private boolean yamlToJson = false;

    @Parameter(required = true, defaultValue = "${project.build.directory}/classes")
    private File yamlToJsonOutputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        StandaloneCodegenerator generator = StandaloneCodegenerator.builder().withApiFilePath(apiFile)
                                                                   .forLanguage(language)
                                                                   .writeResultsTo(outputDirectory)
                                                                   .withApiPackage(apiPackage)
                                                                   .withModelPackage(modelPackage)
                                                                   .withLogger(new MojoCodegeneratorLogger(getLog()))
                                                                   .skipModelgeneration(skipModelgeneration).build();

        try {
            generator.generate();

            //
            project.addCompileSourceRoot(generator.getOutputDirectoryPath());

            if (yamlToJson) {
                YamlToJson converter = YamlToJson.builder().withYamlInputPath(apiFile)
                                                 .withCodegeneratorLogger(new MojoCodegeneratorLogger(getLog()))
                                                 .withOutputDirectoryPath(yamlToJsonOutputDirectory.getAbsolutePath())
                                                 .build();
                converter.convert();
            }

        } catch (CodegenerationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}

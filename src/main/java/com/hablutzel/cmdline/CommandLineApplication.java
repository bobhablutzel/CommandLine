/**
 * Copyright (c) Bob Hablutzel. All rights reserved.
 *
 * This code is released under a simplified BSD license.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.hablutzel.cmdline;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.cli.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Bob Hablutzel on 6/10/16.
 */
public class CommandLineApplication {


    private enum MethodType {
        Boolean, Scalar, Array, List
    };

    /**
     * Private class used to remember configuration values
     * for the methods used with the command line options
     */
    private static final class CommandLineMethodHelper {
        Method method;
        MethodType methodType;
        Class<?> elementType;
        Converter converter;

        CommandLineMethodHelper(Method method, MethodType methodType, Class<?> elementType, Converter converter) {
            this.method = method;
            this.methodType = methodType;
            this.elementType = elementType;
            this.converter = converter;
        }


        // Invokes the method. If any invocation returns false, then
        // the looping stops and false is returned. Otherwise this method
        // returns true. Note that we've already validated that the only
        // possible return is a boolean

        boolean invokeMethod( Object instance, String[] arguments ) throws CommandLineException {
            boolean continueToInvoke = true;
            try {
                switch (methodType) {
                    case Boolean: {
                        Object result = method.invoke(instance);
                        if (result instanceof Boolean) {
                            continueToInvoke = ((Boolean)result);
                        }
                        break;
                    }
                    case Scalar: {
                        if (arguments == null) {
                            method.invoke(instance, new Object[] { null });
                        } else {
                            for (String s : arguments) {
                                Object result = method.invoke(instance, converter.convert(elementType, s));
                                if (result instanceof Boolean) {
                                    continueToInvoke = ((Boolean) result);
                                }
                                if (!continueToInvoke) break;
                            }
                        }
                        break;
                    }
                    case Array: {
                        Object result;
                        if (arguments == null) {
                            result = method.invoke(instance,Array.newInstance(elementType,0));
                        } else {
                            Object array = Array.newInstance(elementType, arguments.length);
                            for (int i = 0; i < arguments.length; ++i) {
                                Array.set(array, i, converter.convert(elementType, arguments[i]));
                            }
                            result = method.invoke(instance, array);
                        }
                        if (result instanceof Boolean) {
                            continueToInvoke = ((Boolean) result);
                        }
                        break;
                    }
                    case List: {
                        Object result;
                        if (arguments == null) {
                            result = method.invoke(instance,new ArrayList());
                        } else {
                            List list = new ArrayList();
                            for (int i = 0; i < arguments.length; ++i) {
                                list.add(converter.convert(elementType, arguments[i]));
                            }
                            result = method.invoke(instance, list);
                        }
                        if (result instanceof Boolean) {
                            continueToInvoke = ((Boolean) result);
                        }
                        break;
                    }
                }
            } catch (InvocationTargetException e) {
                throw new CommandLineException("Unable to invoke method " + method.getName(), e);
            } catch (IllegalAccessException e) {
                throw new CommandLineException("Unable to invoke method " + method.getName() + " because the method is not accessible");
            }

            return continueToInvoke;
        }

    }


    /**
     * Common-cli options for command line parsing
     */
    private Options options = new Options();

    /**
     * Map of options to configurations
     */
    private Map<Option,CommandLineMethodHelper> optionHelperMap = new HashMap<>();


    /**
     * Helper for the main command line method
     */
    private CommandLineMethodHelper mainHelper = null;


    /**
     * This method scans the subclass for annotations
     * that denote the command line options and arguments,
     * and configures the systems so that the members that
     * have been annotated in that way are set up for calling
     * at command line processing time
     *
     */
    private final void configure() throws CommandLineException {

        // Find all the fields in our subclass
        for (Method method: this.getClass().getDeclaredMethods()) {

            // If this method is marked with a  command line option, then configure
            // a corresponding commons-cli command line option here
            if (method.isAnnotationPresent(CommandLineOption.class)) {
                CommandLineOption commandLineOption = method.getDeclaredAnnotation(CommandLineOption.class);
                if (commandLineOption != null) {

                    // Get the basic information about the option - the name and description
                    String shortName = commandLineOption.shortForm().equals("") ? null : commandLineOption.shortForm();
                    String longName = commandLineOption.longForm().equals("") ? null : commandLineOption.longForm();
                    String description = commandLineOption.usage();

                    // If both the short and long name are null, then use the field name as the long name
                    if (shortName == null && longName == null) {
                        longName = method.getName();
                    }

                    // The signature of the method determines what kind of command line
                    // option is allowed. Basically, if the method does not take an argument,
                    // then the option does not take arguments either. In this case, the
                    // method is just called when the option is present.
                    //
                    // If the method does take argument, there are restrictions on the arguments
                    // that are allowed. If there is a single argument, then the method will be
                    // called for each argument supplied to the option. Generally in this case you
                    // want the maximum number of option arguments to be 1, and you are just getting
                    // the value of the argument. On the other hand, if the single argument is either
                    // and array or a List<>, then the arguments will be passed in as an argument
                    // or list respectively.
                    //
                    // Methods with more than 1 argument are not allowed. Methods with return types
                    // other than boolean are not allowed. Methods that throw an exception other than
                    // org.apache.commons.cli.CommandLineException are not allowed,
                    //
                    // If the method returns a boolean, and calling that method returns FALSE, then the
                    // command line main function will not be called.
                    //
                    // The class of the argument has to be convertable using common-beanutils
                    // conversion facilities
                    CommandLineMethodHelper helper = getHelperForCommandOption( method, commandLineOption );

                    // Now create and configure an option based on what the method is capable of handling
                    // and the command line option parameters
                    boolean allowsArguments = helper.methodType != MethodType.Boolean;
                    Option option = new Option( shortName, longName, allowsArguments, description );

                    // Configure it
                    option.setRequired(commandLineOption.required());
                    if (option.hasArg()) {
                        option.setType(helper.elementType);
                        option.setArgs(commandLineOption.maximumArgumentCount());
                        option.setValueSeparator(commandLineOption.argumentSeparator());
                        option.setOptionalArg(commandLineOption.optionalArgument());
                    }

                    // Remember it, both in the commons-cli options set and
                    // in our list of elements for later post-processing
                    options.addOption(option);
                    optionHelperMap.put(option, helper);
                }

            // This was not a command line option method - is it the main command line method?
            } else if (method.isAnnotationPresent(CommandLineMain.class)) {

                // Make sure we only have one
                if (mainHelper != null) {
                    throw new CommandLineException( "Cannot have two main methods specified" );
                } else {
                    mainHelper = getHelperForCommandLineMain(method);
                }
            }
        }
    }

    /**
     * Validate a Method to be a main command line application method.
     *
     * Methods with more than 1 argument are not allowed. Methods with return types
     * are not allowed. Methods that throw an exception other than
     * org.apache.commons.cli.CommandLineException are not allowed,
     *
     * @param method the method to validate
     * @return A new method helper for the method
     */
    private CommandLineMethodHelper getHelperForCommandLineMain(Method method) throws CommandLineException {

        // Validate that the return type is a void
        if (!method.getReturnType().equals(Void.TYPE)) {
            throw new CommandLineException("For method " + method.getName() + ", the return type is not void");
        }

        // Validate the exceptions throws by the method
        for (Class<?> clazz : method.getExceptionTypes()) {
            if (!clazz.equals(CommandLineException.class)) {
                throw new CommandLineException("For method " + method.getName() + ", there is an invalid exception class " + clazz.getName());
            }
        }

        // In order to get ready to create the configuration instance,
        // we will need to know the command line option type
        // and the element type.
        Class<?> elementClass;
        MethodType methodType;
        Converter converter;

        // Get the parameters of the method. We'll use these to
        // determine what type of option we have - scalar, boolean, etc.
        Class<?> parameterClasses[] = method.getParameterTypes();

        // See what the length tells us
        switch (parameterClasses.length) {
            case 0:
                throw new CommandLineException("Main command line method must take arguments" );
            case 1: {

                // For a method with one argument, we have to look
                // more closely at the argument. It has to be a simple
                // scalar object, an array, or a list.
                Class<?> parameterClass = parameterClasses[0];
                if (parameterClass.isArray()) {

                    // For an array, we get the element class based on the
                    // underlying component type
                    methodType = MethodType.Array;
                    elementClass = parameterClass.getComponentType();
                } else {

                    // For a scalar, we get the element type from the
                    // type of the parameter.
                    methodType = MethodType.Scalar;
                    elementClass = parameterClass.getClass();
                }

                // Now that we have the element type, make sure it's convertable
                converter = ConvertUtils.lookup(String.class, elementClass);
                if (converter == null) {
                    throw new CommandLineException("Cannot find a conversion from String to " + elementClass.getName() + " for method " + method.getName());
                }
                break;
            }
            default: {

                // Other method types not allowed.
                throw new CommandLineException("Method " + method.getName() + " has too many arguments");
            }
        }

        // Now we can return the configuration for this method
        return new CommandLineMethodHelper(method, methodType, elementClass, converter);
    }


    /**
     * Validate a Method to be a command line option methods.
     *
     * Methods with more than 1 argument are not allowed. Methods with return types
     * other than boolean are not allowed. Methods that throw an exception other than
     * org.apache.commons.cli.CommandLineException are not allowed,
     *
     * @param method the method to validate
     * @param commandLineOption the options on that method
     * @return A new method helper for the method
     */
    private CommandLineMethodHelper getHelperForCommandOption(Method method, CommandLineOption commandLineOption) throws CommandLineException {

        // Validate that the return type is a boolean or void
        if (!method.getReturnType().equals(Boolean.TYPE) && !method.getReturnType().equals(Void.TYPE)) {
            throw new CommandLineException("For method " + method.getName() + ", the return type is not boolean or void");
        }

        // Validate the exceptions throws by the method
        for (Class<?> clazz : method.getExceptionTypes()) {
            if (!clazz.equals(CommandLineException.class)) {
                throw new CommandLineException("For method " + method.getName() + ", there is an invalid exception class " + clazz.getName());
            }
        }

        // In order to get ready to create the configuration instance,
        // we will need to know the command line option type
        // and the element type.
        Class<?> elementClass = null;
        MethodType methodType;
        Converter converter;

        // Get the parameters of the method. We'll use these to
        // determine what type of option we have - scalar, boolean, etc.
        Class<?> parameterClasses[] = method.getParameterTypes();

        // See what the length tells us
        switch (parameterClasses.length) {
            case 0:
                methodType = MethodType.Boolean;
                converter = null;
                break;
            case 1: {

                // For a method with one argument, we have to look
                // more closely at the argument. It has to be a simple
                // scalar object, an array, or a list.
                Class<?> parameterClass = parameterClasses[0];
                if (parameterClass.isArray()) {

                    // For an array, we get the element class based on the
                    // underlying component type
                    methodType = MethodType.Array;
                    elementClass = parameterClass.getComponentType();
                } else if (List.class.isAssignableFrom(parameterClass)) {

                    // For a list, we get the element class from the command
                    // line options annotation
                    methodType = MethodType.List;
                    elementClass = commandLineOption.argumentType();
                } else {

                    // For a scalar, we get the element type from the
                    // type of the parameter.
                    methodType = MethodType.Scalar;
                    elementClass = parameterClass.getClass();
                }

                // Now that we have the element type, make sure it's convertable
                converter = ConvertUtils.lookup(String.class, elementClass);
                if (converter == null) {
                    throw new CommandLineException("Cannot find a conversion from String to " + elementClass.getName() + " for method " + method.getName());
                }
                break;
            }
            default: {

                // Other method types not allowed.
                throw new CommandLineException("Method " + method.getName() + " has too many arguments");
            }
        }

        // Now we can return the configuration for this method
        return new CommandLineMethodHelper(method, methodType, elementClass, converter);
    }


    /**
     * Method for running the command line application.
     *
     * @param args The arguments passed into main()
     * @throws CommandLineException
     */
    public void parseAndRun(String args[] ) throws CommandLineException {

        // Configure our environment
        configure();

        // Make sure there is a main helper
        if (mainHelper == null) {
            throw new CommandLineException("You must specify the main method with @CommandLineMain" );
        }
        CommandLine line = null;
        CommandLineParser parser = null;

        try {
            // Parse the command line
            parser = new DefaultParser();
            line = parser.parse(options, args );
        } catch (ParseException e) {
            throw new CommandLineException("Unable to parse command line", e);
        } finally {
            parser = null;
        }

        // Assume we're continuing
        boolean runMain = true;

        // Loop through all our known options
        for (Map.Entry<Option,CommandLineMethodHelper> entry : optionHelperMap.entrySet()) {

            // See if this option was specified
            Option option = entry.getKey();
            CommandLineMethodHelper helper = entry.getValue();
            boolean present = option.getOpt() == null || option.getOpt().equals("")
                    ? line.hasOption(option.getLongOpt())
                    : line.hasOption(option.getOpt());
            if (present) {

                // The user specified this option. Now we have to handle the
                // values, if it has any
                if (option.hasArg()) {
                    String[] arguments = option.getOpt() == null || option.getOpt().equals("")
                            ? line.getOptionValues(option.getLongOpt())
                            : line.getOptionValues(option.getOpt());
                    runMain = helper.invokeMethod(this,arguments) && runMain;
                } else {
                    runMain = helper.invokeMethod(this, new String[] {}) && runMain;
                }
            }
        }

        // Now handle all the extra arguments. In order to clean up memory,
        // we get rid of the structures that we needed in order to parse
        if (runMain) {

            // Get a reference to the arguments
            String[] arguments = line.getArgs();

            // Clean up the parsing variables
            line = null;
            optionHelperMap = null;

            // Now call the main method. This means we have to keep
            // the main helper around, but that's small
            mainHelper.invokeMethod(this, arguments);
        }
    }


    /**
     * Helper function to print the command line usage
     *
     * @param appName Name of the application
     * @param header Header line
     * @param footer Footer line
     */
    public void printCommandLineUsageText(String appName, String header, String footer) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(appName, header, options, footer, true);
    }
}

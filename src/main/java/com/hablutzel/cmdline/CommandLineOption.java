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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by bob on 6/11/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandLineOption {

    /** The short name of the argument. This will
     * be a single character and will
     * be exposed via -C, where C is the character
     * in question.
     */
    String shortForm() default "";

    /**
     * The long form of the argument. This will
     * be an arbitrary string and will be exposed
     * via --STRING, where STRING is the value
     * of longForm
     */
    String longForm() default "";

    /**
     * A comment for the printCommandLineUsageText text. This is
     * required so that the help message can be created.
     * This is the only required parameter
     * @return The string to display for help
     */
    String usage();

    /**
     * The argument type. This type must be
     * convertable from a String via a Commons
     * BeanUtils converter class. This can only
     * be specified if the field this annotation
     * is associated with is a Collection class.
     */
    Class<?> argumentType() default Void.class;


    /**
     * Determines if the option is required on
     * the command line. By default this is false.
     *
     * @return boolean value
     */
    boolean required() default false;


    /**
     * If the argumentType is either a collection
     * class or an array class, the argument separator
     * can be used to indicate the separator to use
     * between items. By default this is a comma.
     *
     * @return the separator to use
     */
    char argumentSeparator() default ',';


    /**
     * Define the maximum argument count for this option
     *
     * @return
     */
    int maximumArgumentCount() default 1;


    /**
     * For options with arguments only, determines whether
     * the argument is optional or not.
     *
     * @return
     */
    boolean optionalArgument() default false;
}

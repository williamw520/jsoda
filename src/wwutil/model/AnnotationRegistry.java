/******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is: Jsoda
 * The Initial Developer of the Original Code is: William Wong (williamw520@gmail.com)
 * Portions created by William Wong are Copyright (C) 2012 William Wong, All Rights Reserved.
 *
 ******************************************************************************/


package wwutil.model;


import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.concurrent.*;



/**
 * Annotation handlers registry
 */
public class AnnotationRegistry
{
    // Handler registry
    private Map<Class, AnnotationClassHandler>  classHandlers = new ConcurrentHashMap<Class, AnnotationClassHandler>();   // map of annotation to Handlers
    private Map<Class, AnnotationFieldHandler>  fieldHandlers = new ConcurrentHashMap<Class, AnnotationFieldHandler>();   // map of annotation to Handlers


    public AnnotationRegistry register(Class annotationClass, AnnotationClassHandler handler) {
        classHandlers.put(annotationClass, handler);
        return this;
    }

    public AnnotationRegistry register(Class annotationClass, AnnotationFieldHandler handler) {
        fieldHandlers.put(annotationClass, handler);
        return this;
    }

    public void applyClassHandlers(Object obj) {
        if (obj == null)
            return;

        Class   objClass = obj.getClass();
        for (Annotation annObj : objClass.getAnnotations()) {
            AnnotationClassHandler  handler = classHandlers.get(annObj.annotationType());
            if (handler != null) {
                handler.handle(annObj, obj);
            }
        }
    }

    public void checkModelOnFields(Field[] allFields) {
        for (Field field : allFields) {
            for (Annotation annObj : field.getDeclaredAnnotations()) {
                AnnotationFieldHandler  handler = fieldHandlers.get(annObj.annotationType());
                if (handler != null) {
                    handler.checkModel(annObj, field);
                }
            }
        }
    }
    
    public void applyFieldHandlers(Object obj, Field[] allFields) {
        if (obj == null)
            return;

        for (Field field : allFields) {
            for (Annotation annObj : field.getDeclaredAnnotations()) {
                AnnotationFieldHandler  handler = fieldHandlers.get(annObj.annotationType());
                if (handler != null) {
                    handler.handle(annObj, obj, field);
                }
            }
        }
    }

}


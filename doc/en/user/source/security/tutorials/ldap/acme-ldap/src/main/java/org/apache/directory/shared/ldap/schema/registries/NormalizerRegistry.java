/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.schema.registries;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * Normalizer registry service class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $
 */
public interface NormalizerRegistry extends SchemaObjectRegistry<Normalizer>,
    Iterable<Normalizer>
{
    /**
     * Registers a new Normalizer with this registry.
     *
     * @param normalizer the Normalizer to register
     * @throws LdapException if the Normalizer is already registered or
     * the registration operation is not supported
     */
    void register( Normalizer normalizer ) throws LdapException;


    /**
     * Removes the Normalizer registered with this registry, using its
     * numeric OID.
     * 
     * @param numericOid the numeric identifier
     * @throws LdapException if the numeric identifier is invalid
     */
    Normalizer unregister( String numericOid ) throws LdapException;
    
    
    /**
     * Unregisters all Normalizers defined for a specific schema from
     * this registry.
     * 
     * @param schemaName the name of the schema whose Normalizers will be removed from
     */
    void unregisterSchemaElements( String schemaName ) throws LdapException;
    
    
    /**
     * Copy the NormalizerRegistry
     */
    NormalizerRegistry copy();
}

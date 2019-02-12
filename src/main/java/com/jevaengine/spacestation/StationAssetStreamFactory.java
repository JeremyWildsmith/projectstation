/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.jevaengine.spacestation;

import io.github.jevaengine.IAssetStreamFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

public class StationAssetStreamFactory implements IAssetStreamFactory
{
	private final File m_assetSource;
	
	public StationAssetStreamFactory(URI assetSource)
	{
	  m_assetSource = new File(assetSource);
	}
	
	private File resolvePath(String relativePath)
	{
		if(relativePath.startsWith("/"))
			relativePath = "." + relativePath;

		File file = new File(m_assetSource, relativePath);
			
		if(file.exists())
			return file;
		
		return new File(relativePath);
	}
	
	@Override
	public InputStream create(URI path) throws AssetStreamConstructionException
	{
		try
		{
			if("local".equals(path.getScheme()))
			{
				String classPath = path.getPath().startsWith("/") ? path.getPath().substring(1) : path.getPath();
				
				InputStream is = this.getClass().getClassLoader().getResourceAsStream(classPath);
			
				if (is == null)
					throw new AssetStreamConstructionException(path, new UnresolvedResourcePathException());
				
				return is;
			}
			else
			{
				return new FileInputStream(resolvePath(path.getPath()));
			}
		} catch (FileNotFoundException ex)
		{
			throw new AssetStreamConstructionException(path, new UnresolvedResourcePathException());
		}
	}
	
	public final class UnresolvedResourcePathException extends Exception
	{
		private static final long serialVersionUID = 1L;

		private UnresolvedResourcePathException() { }
	}
	
	public final class NoRootAssignedException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		private NoRootAssignedException()
		{
			super("No root directory has been assigned!");
		}
	}
}
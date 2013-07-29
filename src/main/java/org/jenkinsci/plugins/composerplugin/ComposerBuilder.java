package org.jenkinsci.plugins.composerplugin;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import hudson.util.NullStream;
import hudson.util.StreamTaskListener;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 * 
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link ComposerBuilder} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 * 
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 * 
 * @author Ricardo Dias Cavalcante (ricardodc@gmail.com)
 */
public class ComposerBuilder extends Builder {

	private final String name;

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public ComposerBuilder(String name) {
		this.name = name;
	}

	/**
	 * We'll use this from the <tt>config.jelly</tt>.
	 */
	public String getName() {
		return name;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) {
			listener.getLogger().println(
					"Aways update Composer on build, " + name + "!");

		return true;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link ComposerBuilder}. Used as a singleton. The class is
	 * marked as public so that it can be accessed from views.
	 * 
	 * <p>
	 * See
	 * <tt>src/main/resources/hudson/plugins/hello_world/ComposerBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {

		@CopyOnWrite
		private volatile ComposerInstallation[] installations = new ComposerInstallation[0];

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 */
		public FormValidation doCheckName(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a name");
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types

			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Composer Plugin";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData)
				throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
//			awaysUpdateComposer = formData.getBoolean("awaysUpdateComposer");
//
//			awaysUpdateProject = formData.getBoolean("awaysUpdateProject");
			// ^Can also use req.bindJSON(this, formData);
			// (easier when there are many fields; need set* methods for this,
			// like setUseFrench)
			save();
			return super.configure(req, formData);
		}

		/**
		 * This method returns true if the global configuration says we should
		 * speak French.
		 * 
		 * The method name is bit awkward because global.jelly calls this method
		 * to determine the initial state of the checkbox by the naming
		 * convention.
		 */

		public ComposerInstallation[] getInstallations() {
			return installations;
		}

		public void setInstallations(ComposerInstallation... installations) {
			List<ComposerInstallation> tmpList = new ArrayList<ComposerBuilder.ComposerInstallation>();
			// remote empty composer installation :
			if (installations != null) {
				Collections.addAll(tmpList, installations);
				for (ComposerInstallation installation : installations) {
					if (Util.fixEmptyAndTrim(installation.getName()) == null) {
						tmpList.remove(installation);
					}
				}
			}
			this.installations = tmpList
					.toArray(new ComposerInstallation[tmpList.size()]);
			save();
		}

	}

	/**
	 * Represents a Composer installation in a Jenkins.
	 */
	public static final class ComposerInstallation extends ToolInstallation
			implements EnvironmentSpecific<ComposerInstallation>,
			NodeSpecific<ComposerInstallation> {

		private String composerHome;

		@DataBoundConstructor
		public ComposerInstallation(String name, String home,
				List<? extends ToolProperty<?>> properties) {
			super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home),
					properties);
		}

		public File getHomeDir() {
			return new File(getHome());
		}

		/**
		 * Configuration of enviroment variables
		 */
		@Override
		public void buildEnvVars(EnvVars env) {
			String home = getHome();
			env.put("COMPOSER_HOME", home);
			env.put("COMPOSER_ROOT_VERSION", home);
			env.put("COMPOSER_VENDOR", home + "vendor/bin");
		}

		/**
		 * Gets the executable path of this composer on the given target system. If
		 * it is installed in the system. gets its return
		 */
		public String getExecutable(Launcher launcher) throws IOException,
				InterruptedException {
			return launcher.getChannel().call(
					new Callable<String, IOException>() {
						private static final long serialVersionUID = 2342342340812310384L;

						public String call() throws IOException {
							File exe = getExeFile("composer");
							if (exe.exists())
								return exe.getPath();
							exe = getExeFile("composer");
							if (exe.exists())
								return exe.getPath();
							return null;
						}
					});
		}

		private File getExeFile(String execName) {
			if (File.separatorChar == '\\')
				execName += ".bat";

			String composerHome = Util.replaceMacro(getHome(),
					EnvVars.masterEnvVars);

			return new File(composerHome, "bin/" + execName);
		}

		/**
		 * Returns true if the executable exists.
		 */
		public boolean getExists() {
			try {
				return getExecutable(new LocalLauncher(new StreamTaskListener(
						new NullStream()))) != null;
			} catch (IOException e) {
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}

		private static final long serialVersionUID = 1239874089712L;

		public ComposerInstallation forEnvironment(EnvVars environment) {
			return new ComposerInstallation(getName(),
					environment.expand(getHome()), getProperties().toList());
		}

		public ComposerInstallation forNode(Node node, TaskListener log)
				throws IOException, InterruptedException {
			return new ComposerInstallation(getName(), translateFor(node, log),
					getProperties().toList());
		}

		@Extension
		public static class DescriptorImpl extends
				ToolDescriptor<ComposerInstallation> {
			@Override
			public String getDisplayName() {
				return "Composer Installation";
			}

			@Override
			public List<? extends ToolInstaller> getDefaultInstallers() {
				return Collections.singletonList(new ComposerInstaller(null));
			}

			// overriding them for backward compatibility.
			// newer code need not do this
			@Override
			public ComposerInstallation[] getInstallations() {
				return Jenkins
						.getInstance()
						.getDescriptorByType(
								ComposerBuilder.DescriptorImpl.class)
						.getInstallations();
			}

			// overriding them for backward compatibility.
			// newer code need not do this
			@Override
			public void setInstallations(ComposerInstallation... installations) {
				Jenkins.getInstance()
						.getDescriptorByType(
								ComposerBuilder.DescriptorImpl.class)
						.setInstallations(installations);
			}

			/**
			 * Checks if the COMPOSER_HOME is valid.
			 */
			public FormValidation doCheckMavenHome(@QueryParameter File value) {
				// this can be used to check the existence of a file on the
				// server, so needs to be protected
				if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER))
					return FormValidation.ok();

				if (value.getPath().equals(""))
					return FormValidation.ok();

				// Composer might not be a directory. Its a phar executed by php
				// if(!value.isDirectory())
				// return
				// FormValidation.error(Messages.Maven_NotADirectory(value));
				// return FormValidation.error("This ");

				File composerPharFile = new File(value, "composer.phar");

				if (!composerPharFile.exists())
					return FormValidation.error("Not a valid ");

				return FormValidation.ok();
			}

			public FormValidation doCheckName(@QueryParameter String value) {
				return FormValidation.validateRequired(value);
			}
		}

		public static class ConverterImpl extends ToolConverter {
			public ConverterImpl(XStream2 xstream) {
				super(xstream);
			}

			@Override
			protected String oldHomeField(ToolInstallation obj) {
				return ((ComposerInstallation) obj).composerHome;
			}
		}
	}
	
	/**
     * Automatic Composer installer from apache.org.
     */
    public static class ComposerInstaller extends DownloadFromUrlInstaller {
    	
        @DataBoundConstructor
        public ComposerInstaller(String id) {
            super(id);
        }

        @Extension
        public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<ComposerInstaller> {
            public String getDisplayName() {
                return "Install from internet";
            }

            @Override
            public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
                return toolType==ComposerInstallation.class;
            }
        }
    }

}

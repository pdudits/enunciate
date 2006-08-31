package net.sf.enunciate.config;

import net.sf.enunciate.modules.DeploymentModule;

import java.util.Comparator;

/**
 * Compares deployment modules for the order in which they should be executed.
 *
 * @author Ryan Heaton
 */
public class DeploymentModuleComparator implements Comparator<DeploymentModule> {

  /**
   * Compares modules by order, then by namespace, then by name.
   * 
   * @param module1 The first module.
   * @param module2 The second module.
   * @return The comparison.
   */
  public int compare(DeploymentModule module1, DeploymentModule module2) {
    int comparison = module1.getOrder() - module2.getOrder();

    if (comparison == 0) {
      String namespace1 = module1.getNamespace() == null ? "" : module1.getNamespace();
      String namespace2 = module2.getNamespace() == null ? "" : module2.getNamespace();

      comparison = namespace1.compareTo(namespace2);
      if (comparison == 0) {
        String name1 = module1.getName() == null ? "" : module1.getName();
        String name2 = module2.getName() == null ? "" : module2.getName();
        comparison = name1.compareTo(name2);
      }
    }

    return comparison;
  }
}

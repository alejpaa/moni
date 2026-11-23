import {
  ArrowDownLeft,
  ArrowUpRight,
  Briefcase,
  Bus,
  Clapperboard,
  Coffee,
  Fuel,
  GraduationCap,
  Gift,
  HeartPulse,
  House,
  PiggyBank,
  Plane,
  ShoppingCart,
  Utensils,
  Zap,
  type LucideIcon,
} from "lucide-react";
import { categoryIconKey, type CategoryIconKey, type TransactionType } from "@moni/shared";

const ICONS: Record<CategoryIconKey, LucideIcon> = {
  briefcase: Briefcase,
  utensils: Utensils,
  "shopping-cart": ShoppingCart,
  coffee: Coffee,
  bus: Bus,
  fuel: Fuel,
  house: House,
  zap: Zap,
  "heart-pulse": HeartPulse,
  clapperboard: Clapperboard,
  gift: Gift,
  plane: Plane,
  "graduation-cap": GraduationCap,
  "piggy-bank": PiggyBank,
  "arrow-down-left": ArrowDownLeft,
  "arrow-up-right": ArrowUpRight,
};

export default function CategoryIcon({ category, type }: { category: string; type: TransactionType }) {
  const Icon = ICONS[categoryIconKey(category, type)];
  return (
    <span className="flex size-9 shrink-0 items-center justify-center rounded-[10px] bg-acc-tile text-acc-ink">
      <Icon size={18} strokeWidth={2} aria-hidden />
    </span>
  );
}

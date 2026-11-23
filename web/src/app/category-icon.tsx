import {
  ArrowDownLeft,
  ArrowUpRight,
  Briefcase,
  Repeat,
  RotateCcw,
  ShoppingBag,
  Store,
  Banknote,
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
import { categoryIconKey, type TransactionType } from "@moni/shared";

const ICONS: Record<string, LucideIcon> = {
  "shopping-bag": ShoppingBag,
  store: Store,
  repeat: Repeat,
  "rotate-ccw": RotateCcw,
  cash: Banknote,
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

/** Renders the category's stored Lucide `icon`; falls back to the keyword mapping when null/unknown. */
export default function CategoryIcon({
  category,
  icon,
  type,
}: {
  category: string;
  icon?: string | null;
  type: TransactionType;
}) {
  const Icon = (icon && ICONS[icon]) || ICONS[categoryIconKey(category, type)];
  return (
    <span className="flex size-9 shrink-0 items-center justify-center rounded-[10px] bg-acc-tile text-acc-ink">
      <Icon size={18} strokeWidth={2} aria-hidden />
    </span>
  );
}
